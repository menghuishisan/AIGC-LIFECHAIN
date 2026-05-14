package com.lifechain.infra.attachment;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件视图对象
 */
@Data
public class AttachmentVO {
    private String bizType;
    private String bizNo;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadTime;

    public static AttachmentVO fromEntity(SysAttachmentEntity entity) {
        AttachmentVO vo = new AttachmentVO();
        vo.setBizType(entity.getBizType());
        vo.setBizNo(entity.getBizNo());
        vo.setFileName(entity.getFileName());
        vo.setFileSize(entity.getFileSize());
        vo.setFileType(entity.getFileType());
        vo.setUploadTime(entity.getUploadTime());
        return vo;
    }
}
