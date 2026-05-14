package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作品文件实体
 * <p>
 * 对应数据库表 {@code work_file}，存储作品关联的文件信息，
 * 包括原始文件、缩略图、预览文件等不同用途的文件记录。
 * 每个作品可关联多个文件。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_file")
public class WorkFileEntity extends BaseEntity {

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 文件名 */
    @TableField("file_name")
    private String fileName;

    /** 存储路径 */
    @TableField("file_path")
    private String filePath;

    /** 文件大小（字节） */
    @TableField("file_size")
    private Long fileSize;

    /** 文件类型 */
    @TableField("file_type")
    private String fileType;

    /** 文件哈希（SHA-256） */
    @TableField("file_hash")
    private String fileHash;

    /** 访问地址 */
    @TableField("file_url")
    private String fileUrl;

    /** 用途（ORIGINAL/THUMBNAIL/PREVIEW） */
    @TableField("purpose")
    private String purpose;
}
