package com.lifechain.infra.storage;

import java.io.InputStream;

/**
 * 对象存储服务接口
 * <p>
 * 抽象文件上传、下载、删除及签名URL生成操作，
 * 底层可对接MinIO或其他S3兼容存储。
 * </p>
 */
public interface StorageService {

    /**
     * 上传文件（流方式）
     *
     * @param objectName  对象名称（含路径前缀），如 works/2024/abc.png
     * @param inputStream 文件输入流
     * @param size        文件字节大小
     * @param contentType MIME类型，如 image/png
     * @return 文件的完整访问URL
     */
    String uploadFile(String objectName, InputStream inputStream, long size, String contentType);

    /**
     * 上传文件（字节数组方式）
     *
     * @param objectName  对象名称（含路径前缀）
     * @param data        文件字节数组
     * @param contentType MIME类型
     * @return 文件的完整访问URL
     */
    String uploadFile(String objectName, byte[] data, String contentType);

    /**
     * 查询对象元数据
     *
     * @param objectName 对象名称
     * @return 对象元数据
     */
    StorageObjectMetadata statObject(String objectName);

    /**
     * 下载文件
     *
     * @param objectName 对象名称
     * @return 文件输入流，调用方负责关闭
     */
    InputStream downloadFile(String objectName);

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     */
    void deleteFile(String objectName);

    /**
     * 获取签名下载URL（临时访问）
     *
     * @param objectName    对象名称
     * @param expireMinutes 有效期（分钟）
     * @return 带签名的临时下载URL
     */
    String getPresignedUrl(String objectName, int expireMinutes);

    /**
     * 获取签名上传URL（前端直传）
     *
     * @param objectName    对象名称
     * @param expireMinutes 有效期（分钟）
     * @return 带签名的临时上传URL
     */
    String getUploadPresignedUrl(String objectName, int expireMinutes);
}
