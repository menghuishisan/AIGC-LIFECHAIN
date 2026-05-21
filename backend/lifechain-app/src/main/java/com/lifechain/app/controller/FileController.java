package com.lifechain.app.controller;

import com.lifechain.app.dto.FileCallbackRequest;
import com.lifechain.app.dto.UploadPolicyCache;
import com.lifechain.app.dto.UploadPolicyResponse;
import com.lifechain.app.service.FileBindingPermissionService;
import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.infra.attachment.AttachmentService;
import com.lifechain.infra.attachment.AttachmentVO;
import com.lifechain.infra.redis.RedisService;
import com.lifechain.infra.storage.StorageObjectMetadata;
import com.lifechain.infra.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件管理控制器。
 * <p>
 * 负责签发对象存储直传策略，并在上传回调时结合服务端缓存策略、
 * 当前登录用户、对象存储真实元数据与文件哈希完成附件确认。
 * </p>
 */
@Slf4j
@Tag(name = "文件管理", description = "文件上传策略与回调确认")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private static final String UPLOAD_POLICY_CACHE_PREFIX = "upload:policy:";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/quicktime", "video/x-msvideo",
            "audio/mpeg", "audio/wav", "audio/ogg",
            "application/pdf",
            "model/gltf-binary", "model/gltf+json"
    );

    private static final Set<String> ALLOWED_STORAGE_BIZ_TYPES = Set.of(
            "work", "cover", "avatar", "cert", "general"
    );

    private static final Set<String> PUBLIC_BIZ_TYPES = Set.of("cover");

    private final StorageService storageService;
    private final AttachmentService attachmentService;
    private final RedisService redisService;
    private final FileBindingPermissionService fileBindingPermissionService;

    @Value("${app.file.upload-policy-expire-minutes}")
    private int uploadPolicyExpireMinutes;

    /**
     * 获取预签名上传策略。
     *
     * @param fileName 原始文件名
     * @param bizType 上传业务类型
     * @return 上传策略响应
     */
    @GetMapping("/api/files/upload-policy")
    @Operation(summary = "获取上传策略", description = "生成对象存储预签名上传地址，并返回回调确认所需的上传令牌")
    public ApiResponse<UploadPolicyResponse> getUploadPolicy(
            @RequestParam String fileName,
            @RequestParam(defaultValue = "general") String bizType) {
        Long uploaderId = UserContext.getUserId();
        String safeFileName = Paths.get(fileName).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        String storageBizType = normalizeStorageBizType(bizType);
        String objectName = storageBizType + "/" + BizNoUtil.generate("FILE") + "/" + safeFileName;

        boolean isPublic = PUBLIC_BIZ_TYPES.contains(storageBizType);
        String uploadUrl;
        String accessUrl;
        if (isPublic) {
            uploadUrl = storageService.getPublicUploadPresignedUrl(objectName, uploadPolicyExpireMinutes);
            accessUrl = storageService.buildPublicFileUrl(objectName);
        } else {
            uploadUrl = storageService.getUploadPresignedUrl(objectName, uploadPolicyExpireMinutes);
            accessUrl = storageService.getPresignedUrl(objectName, 60);
        }

        String uploadToken = UUID.randomUUID().toString().replace("-", "");
        UploadPolicyCache cache = new UploadPolicyCache();
        cache.setUploadToken(uploadToken);
        cache.setObjectName(objectName);
        cache.setStorageBizType(storageBizType);
        cache.setUploaderId(uploaderId);
        redisService.set(uploadPolicyCacheKey(uploadToken), cache, uploadPolicyExpireMinutes, TimeUnit.MINUTES);

        UploadPolicyResponse response = new UploadPolicyResponse();
        response.setUploadUrl(uploadUrl);
        response.setObjectName(objectName);
        response.setAccessUrl(accessUrl);
        response.setUploadToken(uploadToken);
        return ApiResponse.success(response);
    }

    /**
     * 确认文件上传回调。
     *
     * @param request 上传回调请求
     * @return 附件信息
     */
    @PostMapping("/api/files/callback")
    @Operation(summary = "文件上传回调", description = "校验上传策略、对象元数据与文件哈希，确认后落库附件记录")
    @Idempotent(key = "#request.requestId")
    public ApiResponse<AttachmentVO> uploadCallback(@Valid @RequestBody FileCallbackRequest request) {
        log.info("收到文件上传回调，objectName={}, fileSize={}, contentType={}",
                request.getObjectName(), request.getFileSize(), request.getContentType());

        Long uploaderId = UserContext.getUserId();
        UploadPolicyCache cache = loadAndValidateUploadPolicy(request, uploaderId);
        StorageObjectMetadata metadata = storageService.statObject(request.getObjectName());

        String actualContentType = metadata.getContentType();
        if (actualContentType == null || actualContentType.isBlank()) {
            throw new BizException(ErrorCodeEnum.FILE_TYPE_UNSUPPORTED, "无法识别已上传文件的内容类型");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(actualContentType)) {
            throw new BizException(ErrorCodeEnum.FILE_TYPE_UNSUPPORTED, "不支持的文件类型: " + actualContentType);
        }

        String fileName = extractFileName(request.getObjectName());
        String attachmentBizType = normalizeAttachmentBizType(cache.getStorageBizType());
        if (request.getBizNo() != null && !request.getBizNo().isBlank()) {
            fileBindingPermissionService.validateBindPermission(attachmentBizType, request.getBizNo(), uploaderId);
        }

        var entity = attachmentService.saveAttachment(
                attachmentBizType, null, fileName,
                request.getObjectName(),
                metadata.getSize(),
                actualContentType,
                calculateFileHash(request.getObjectName()), request.getObjectName(), uploaderId);

        if (request.getBizNo() != null && !request.getBizNo().isBlank()) {
            attachmentService.bindAttachmentBiz(entity.getId(), attachmentBizType, request.getBizNo());
        }

        redisService.delete(uploadPolicyCacheKey(request.getUploadToken()));
        return ApiResponse.success(AttachmentVO.fromEntity(entity));
    }

    /**
     * 加载并校验上传策略缓存。
     */
    private UploadPolicyCache loadAndValidateUploadPolicy(FileCallbackRequest request, Long uploaderId) {
        UploadPolicyCache cache = redisService.get(uploadPolicyCacheKey(request.getUploadToken()), UploadPolicyCache.class);
        if (cache == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "上传策略不存在或已过期");
        }
        if (!uploaderId.equals(cache.getUploaderId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "不能确认其他用户的上传结果");
        }
        if (!request.getObjectName().equals(cache.getObjectName())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "对象名称与签发的上传策略不一致");
        }

        String requestStorageBizType = normalizeStorageBizType(
                request.getBizType() == null || request.getBizType().isBlank()
                        ? cache.getStorageBizType()
                        : request.getBizType()
        );
        if (!requestStorageBizType.equals(cache.getStorageBizType())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "业务类型与签发的上传策略不一致");
        }
        return cache;
    }

    /**
     * 从对象名称中提取文件名。
     */
    private String extractFileName(String objectName) {
        String fileName = objectName;
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }
        return fileName;
    }

    /**
     * 规范化对象存储业务类型。
     */
    private String normalizeStorageBizType(String bizType) {
        String normalized = bizType == null || bizType.isBlank() ? "general" : bizType.trim().toLowerCase();
        if (!ALLOWED_STORAGE_BIZ_TYPES.contains(normalized)) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "不支持的上传业务类型: " + bizType);
        }
        return normalized;
    }

    /**
     * 将对象存储业务类型映射为附件业务类型。
     */
    private String normalizeAttachmentBizType(String storageBizType) {
        return switch (storageBizType) {
            case "work" -> "WORK";
            case "cover" -> "WORK";
            case "avatar" -> "ACCOUNT";
            case "cert" -> "CERTIFICATE";
            case "general" -> "GENERAL";
            default -> throw new BizException(ErrorCodeEnum.PARAM_INVALID, "不支持的附件业务类型: " + storageBizType);
        };
    }

    private String uploadPolicyCacheKey(String uploadToken) {
        return UPLOAD_POLICY_CACHE_PREFIX + uploadToken;
    }

    /**
     * 计算对象存储中文件内容的 SHA-256 哈希。
     */
    private String calculateFileHash(String objectName) {
        try (InputStream inputStream = storageService.downloadFile(objectName)) {
            return HashUtil.sha256(inputStream);
        } catch (Exception e) {
            throw new BizException(ErrorCodeEnum.STORAGE_DOWNLOAD_FAILED, "无法计算上传文件的内容哈希");
        }
    }
}
