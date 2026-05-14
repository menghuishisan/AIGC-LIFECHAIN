package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作品AIGC元数据实体
 * <p>
 * 对应数据库表 {@code work_aigc_meta}，存储作品的AIGC生成元信息，
 * 包括AI工具名称、模型、版本、提示词摘要、生成参数及生成时间等。
 * 每个作品对应一条AIGC元数据记录。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_aigc_meta")
public class WorkAigcMetaEntity extends BaseEntity {

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** AIGC工具名称 */
    @TableField("aigc_tool")
    private String aigcTool;

    /** AIGC模型名称 */
    @TableField("aigc_model")
    private String aigcModel;

    /** AIGC版本 */
    @TableField("aigc_version")
    private String aigcVersion;

    /** 提示词摘要 */
    @TableField("prompt_summary")
    private String promptSummary;

    /** 生成参数（JSON） */
    @TableField("generation_params")
    private String generationParams;

    /** 生成时间（UTC） */
    @TableField("generation_time")
    private LocalDateTime generationTime;
}
