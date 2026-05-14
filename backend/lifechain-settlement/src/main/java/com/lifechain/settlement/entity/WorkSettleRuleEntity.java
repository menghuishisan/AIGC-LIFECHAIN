package com.lifechain.settlement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品结算规则实体
 * <p>
 * 对应数据库表 {@code work_settle_rule}，记录单个作品的分账规则配置，
 * 关联结算模板并覆盖创作者与平台的分成比例。
 * 同一作品仅允许一条 ACTIVE 状态的生效规则。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_settle_rule")
public class WorkSettleRuleEntity extends BaseEntity {

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 结算模板ID（可为空，不绑定模板时使用平台/创作者比例） */
    @TableField("template_id")
    private Long templateId;

    /** 创作者账户ID */
    @TableField("creator_account_id")
    private Long creatorAccountId;

    /** 创作者分成比例（如0.8000表示80%） */
    @TableField("creator_ratio")
    private BigDecimal creatorRatio;

    /** 平台分成比例（如0.2000表示20%） */
    @TableField("platform_ratio")
    private BigDecimal platformRatio;

    /** 生效时间（UTC） */
    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    /** 状态（ACTIVE/INACTIVE） */
    @TableField("status")
    private String status;
}
