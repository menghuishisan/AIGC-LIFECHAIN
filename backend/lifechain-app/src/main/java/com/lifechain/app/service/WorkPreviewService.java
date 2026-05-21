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

@Service
@RequiredArgsConstructor
public class WorkPreviewService {

    private final WorkMapper workMapper;
    private final WorkFileMapper workFileMapper;
    private final LicenseRecordMapper licenseRecordMapper;
    private final StorageService storageService;

    private static final int PREVIEW_URL_EXPIRE_MINUTES = 30;
    private static final int LIMITED_DURATION_SECONDS = 30;

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

    private PreviewUrlVO buildPreviewResponse(WorkFileEntity file, String accessLevel) {
        PreviewUrlVO vo = new PreviewUrlVO();
        vo.setAccessLevel(accessLevel);
        vo.setFileType(file.getFileType());
        vo.setFileName(file.getFileName());
        vo.setFileSize(file.getFileSize());

        String fileType = file.getFileType() != null ? file.getFileType().toUpperCase() : "";

        if ("FULL".equals(accessLevel)) {
            vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
            vo.setPreviewDurationSeconds(null);
        } else {
            switch (fileType) {
                case "PNG", "JPG", "JPEG", "GIF", "WEBP" ->  {
                    vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
                }
                case "MP3", "WAV", "OGG", "FLAC" -> {
                    vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
                    vo.setPreviewDurationSeconds(LIMITED_DURATION_SECONDS);
                }
                case "MP4", "MOV", "AVI", "WEBM" -> {
                    vo.setPreviewUrl(storageService.getPresignedUrl(file.getFilePath(), PREVIEW_URL_EXPIRE_MINUTES));
                    vo.setPreviewDurationSeconds(LIMITED_DURATION_SECONDS);
                }
                default -> vo.setPreviewUrl(null);
            }
        }
        return vo;
    }
}