package com.lifechain.app.service;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.infra.storage.StorageService;
import com.lifechain.trade.mapper.LicenseRecordMapper;
import com.lifechain.work.dto.PreviewUrlVO;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.entity.WorkFileEntity;
import com.lifechain.work.mapper.WorkFileMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 作品文件预览服务
 * <p>
 * 根据查看者身份（所有者/已授权买家/市场浏览者）生成不同权限级别的签名预览 URL。
 * 所有者和已授权买家获得完整访问，市场浏览者获得有限预览（图片水印/音视频限时）。
 * </p>
 *
 * @author LifeChain
 */
@Service
@RequiredArgsConstructor
public class WorkPreviewService {

    private final WorkMapper workMapper;
    private final WorkFileMapper workFileMapper;
    private final LicenseRecordMapper licenseRecordMapper;
    private final StorageService storageService;

    private static final int PREVIEW_URL_EXPIRE_MINUTES = 30;
    private static final int LIMITED_DURATION_SECONDS = 30;

    /**
     * 获取作品文件预览URL（需登录）
     * <p>
     * 根据查看者身份判断访问级别，生成对应权限的签名预览 URL。
     * </p>
     *
     * @param workNo          作品编号
     * @param fileId          文件ID
     * @param viewerAccountId 查看者账户ID
     * @return 预览URL及访问级别信息
     */
    public PreviewUrlVO getPreviewUrl(String workNo, Long fileId, Long viewerAccountId) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "作品不存在");
        }

        WorkFileEntity file = workFileMapper.selectById(fileId);
        if (file == null || !file.getWorkId().equals(work.getId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "文件不存在");
        }

        String accessLevel = determineAccessLevel(work, viewerAccountId);
        return buildPreviewResponse(file, accessLevel);
    }

    /**
     * 获取市场作品文件预览URL（公开访问）
     * <p>
     * 市场浏览者统一使用 LIMITED 访问级别，获得有限预览。
     * </p>
     *
     * @param workNo 作品编号
     * @param fileId 文件ID
     * @return 有限预览URL信息
     */
    public PreviewUrlVO getMarketPreviewUrl(String workNo, Long fileId) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "作品不存在");
        }

        WorkFileEntity file = workFileMapper.selectById(fileId);
        if (file == null || !file.getWorkId().equals(work.getId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "文件不存在");
        }

        return buildPreviewResponse(file, "LIMITED");
    }

    /**
     * 判定查看者的访问级别
     * <p>
     * 所有者或持有有效授权的买家返回 FULL，其余返回 LIMITED。
     * </p>
     *
     * @param work            作品实体
     * @param viewerAccountId 查看者账户ID
     * @return "FULL" 或 "LIMITED"
     */
    private String determineAccessLevel(WorkEntity work, Long viewerAccountId) {
        if (viewerAccountId == null) {
            return "LIMITED";
        }
        if (viewerAccountId.equals(work.getCreatorAccountId())) {
            return "FULL";
        }
        if (licenseRecordMapper.existsActiveLicense(work.getId(), viewerAccountId)) {
            return "FULL";
        }
        return "LIMITED";
    }

    /**
     * 构建预览响应对象
     * <p>
     * 根据访问级别和文件类型生成签名 URL，LIMITED 级别对音视频附加时长限制。
     * </p>
     *
     * @param file        文件实体
     * @param accessLevel 访问级别（FULL/LIMITED）
     * @return 预览URL响应对象
     */
    private PreviewUrlVO buildPreviewResponse(WorkFileEntity file, String accessLevel) {
        PreviewUrlVO vo = new PreviewUrlVO();
        vo.setAccessLevel(accessLevel);
        vo.setFileType(file.getFileType());
        vo.setFileName(file.getFileName());
        vo.setFileSize(file.getFileSize());

        String fileType = file.getFileType() != null ? file.getFileType().toUpperCase() : "";

        if ("FULL".equals(accessLevel)) {
            // 完整访问：直接生成签名URL，无时长限制
            vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
            vo.setPreviewDurationSeconds(null);
        } else {
            // 有限访问：根据文件类型区分处理策略
            switch (fileType) {
                case "PNG", "JPG", "JPEG", "GIF", "WEBP" ->  {
                    // 图片类型：生成签名URL，前端叠加水印
                    vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
                }
                case "MP3", "WAV", "OGG", "FLAC" -> {
                    // 音频类型：生成签名URL，限制试听时长
                    vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
                    vo.setPreviewDurationSeconds(LIMITED_DURATION_SECONDS);
                }
                case "MP4", "MOV", "AVI", "WEBM" -> {
                    // 视频类型：生成签名URL，限制试看时长
                    vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
                    vo.setPreviewDurationSeconds(LIMITED_DURATION_SECONDS);
                }
                // 其他类型不提供预览
                default -> vo.setPreviewUrl(null);
            }
        }
        return vo;
    }
}