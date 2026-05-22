package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作品实体
 * <p>
 * 对应数据库表 {@code work}，存储AIGC作品的基本信息，
 * 包括创作者关联、作品标题、类型、状态、文件/元数据哈希及提交时间等。
 * 主键使用雪花ID，作品编号（workNo）为对外唯一标识。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work")
public class WorkEntity extends BaseEntity {

    /** 作品编号（对外唯一标识） */
    @TableField("work_no")
    private String workNo;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;

    /** 创作者账户ID */
    @TableField("creator_account_id")
    private Long creatorAccountId;

    /** 创作者主体ID */
    @TableField("creator_subject_id")
    private Long creatorSubjectId;

    /** 创作者DID ID */
    @TableField("creator_did_id")
    private Long creatorDidId;

    /** 作品标题 */
    @TableField("title")
    private String title;

    /** 作品描述 */
    @TableField("description")
    private String description;

    /** 作品类型（TEXT/IMAGE/AUDIO/VIDEO/MODEL） */
    @TableField("work_type")
    private String workType;

    /** 作品状态 */
    @TableField("status")
    private String status;

    /** 文件哈希（SHA-256） */
    @TableField("file_hash")
    private String fileHash;

    /** 元数据哈希（SHA-256） */
    @TableField("meta_hash")
    private String metaHash;

    /** 封面地址 */
    @TableField("cover_url")
    private String coverUrl;

    /** 提交时间（UTC） */
    @TableField("submit_time")
    private LocalDateTime submitTime;
}
