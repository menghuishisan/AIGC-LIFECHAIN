package com.lifechain.infra.storage;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO对象存储服务实现
 * <p>
 * 基于MinIO Java SDK实现文件上传、下载、删除和签名URL生成。
 * 所有异常统一封装为BizException，便于上层统一处理。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件（流方式）
     *
     * @param objectName  对象名称（含路径前缀），如 works/2024/abc.png
     * @param inputStream 文件输入流
     * @param size        文件字节大小
     * @param contentType MIME类型，如 image/png
     * @return 文件的完整访问URL
     */
    @Override
    public String uploadFile(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            String url = buildFileUrl(objectName);
            log.info("文件上传成功, 对象名: {}, URL: {}", objectName, url);
            return url;
        } catch (Exception e) {
            log.error("文件上传失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文件（字节数组方式）
     *
     * @param objectName  对象名称（含路径前缀）
     * @param data        文件字节数组
     * @param contentType MIME类型
     * @return 文件的完整访问URL
     */
    @Override
    public String uploadFile(String objectName, byte[] data, String contentType) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            return uploadFile(objectName, bais, data.length, contentType);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 查询对象元数据
     *
     * @param objectName 对象名称
     * @return 对象元数据
     */
    @Override
    public StorageObjectMetadata statObject(String objectName) {
        try {
            StatObjectResponse response = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            return StorageObjectMetadata.builder()
                    .objectName(objectName)
                    .size(response.size())
                    .contentType(response.contentType())
                    .etag(response.etag())
                    .build();
        } catch (Exception e) {
            log.error("查询对象元数据失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_DOWNLOAD_FAILED,
                    "查询对象元数据失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param objectName 对象名称
     * @return 文件输入流，调用方负责关闭
     */
    @Override
    public InputStream downloadFile(String objectName) {
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            log.info("文件下载成功, 对象名: {}", objectName);
            return stream;
        } catch (Exception e) {
            log.error("文件下载失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_DOWNLOAD_FAILED,
                    "文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     */
    @Override
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .build());
            log.info("文件删除成功, 对象名: {}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_DELETE_FAILED,
                    "文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取签名下载URL（临时访问）
     *
     * @param objectName    对象名称
     * @param expireMinutes 有效期（分钟）
     * @return 带签名的临时下载URL
     */
    @Override
    public String getPresignedUrl(String objectName, int expireMinutes) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
            log.debug("生成签名下载URL, 对象名: {}, 有效期: {}分钟", objectName, expireMinutes);
            return url;
        } catch (Exception e) {
            log.error("生成签名下载URL失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_DOWNLOAD_FAILED,
                    "生成签名下载URL失败: " + e.getMessage());
        }
    }

    /**
     * 获取签名上传URL（前端直传）
     *
     * @param objectName    对象名称
     * @param expireMinutes 有效期（分钟）
     * @return 带签名的临时上传URL
     */
    @Override
    public String getUploadPresignedUrl(String objectName, int expireMinutes) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
            log.debug("生成签名上传URL, 对象名: {}, 有效期: {}分钟", objectName, expireMinutes);
            return url;
        } catch (Exception e) {
            log.error("生成签名上传URL失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "生成签名上传URL失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadPublicFile(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getPublicBucketName())
                    .object(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build());
            String url = buildPublicFileUrl(objectName);
            log.info("公开文件上传成功, 对象名: {}, URL: {}", objectName, url);
            return url;
        } catch (Exception e) {
            log.error("公开文件上传失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "公开文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String getPublicUploadPresignedUrl(String objectName, int expireMinutes) {
        try {
            ensureBucketExists();
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getPublicBucketName())
                    .object(objectName)
                    .expiry(expireMinutes, TimeUnit.MINUTES)
                    .build());
            log.debug("生成公开桶签名上传URL, 对象名: {}, 有效期: {}分钟", objectName, expireMinutes);
            return url;
        } catch (Exception e) {
            log.error("生成公开桶签名上传URL失败, 对象名: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "生成公开桶签名上传URL失败: " + e.getMessage());
        }
    }

    @Override
    public String buildPublicFileUrl(String objectName) {
        String endpoint = minioConfig.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + minioConfig.getPublicBucketName() + "/" + objectName;
    }

    /**
     * 确保私有桶和公开桶都存在
     */
    private void ensureBucketExists() throws Exception {
        ensureBucket(minioConfig.getBucketName());
        ensurePublicBucket(minioConfig.getPublicBucketName());
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build());
            log.info("创建MinIO桶: {}", bucket);
        }
    }

    private void ensurePublicBucket(String bucket) throws Exception {
        ensureBucket(bucket);
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::%s/*"]
                  }]
                }
                """.formatted(bucket);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(bucket)
                .config(policy)
                .build());
    }

    /**
     * 构建文件的完整访问URL
     *
     * @param objectName 对象名称
     * @return 完整URL
     */
    private String buildFileUrl(String objectName) {
        String endpoint = minioConfig.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + minioConfig.getBucketName() + "/" + objectName;
    }
}
