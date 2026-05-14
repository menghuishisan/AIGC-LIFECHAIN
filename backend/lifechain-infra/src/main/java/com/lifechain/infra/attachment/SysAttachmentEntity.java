package com.lifechain.infra.attachment;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统附件实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_attachment")
public class SysAttachmentEntity extends BaseEntity {

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("biz_no")
    private String bizNo;

    @TableField("file_name")
    private String fileName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("file_type")
    private String fileType;

    @TableField("file_hash")
    private String fileHash;

    @TableField("file_url")
    private String fileUrl;

    @TableField("upload_time")
    private LocalDateTime uploadTime;

    @TableField("uploader_id")
    private Long uploaderId;
}
