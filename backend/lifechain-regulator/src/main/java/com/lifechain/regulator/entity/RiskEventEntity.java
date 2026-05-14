package com.lifechain.regulator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 风险事件实体
 * <p>
 * 对应数据库表 {@code risk_event}，记录系统中发现或上报的风险事件信息，
 * 包含风险目标、风险等级、当前状态及处理结果等核心字段。
 * 风险事件生命周期：标记 → 审查 → 冻结/释放/确认。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("risk_event")
public class RiskEventEntity extends BaseEntity {

    /** 风险编号（对外唯一标识） */
    @TableField("risk_no")
    private String riskNo;

    /** 目标类型（ACCOUNT/WORK/ORDER等） */
    @TableField("target_type")
    private String targetType;

    /** 目标ID */
    @TableField("target_id")
    private Long targetId;

    /** 目标编号 */
    @TableField("target_no")
    private String targetNo;

    /** 风险状态 */
    @TableField("status")
    private String status;

    /** 风险等级（LOW/MEDIUM/HIGH/CRITICAL） */
    @TableField("risk_level")
    private String riskLevel;

    /** 风险类型 */
    @TableField("risk_type")
    private String riskType;

    /** 风险描述 */
    @TableField("risk_description")
    private String riskDescription;

    /** 报告人ID */
    @TableField("reporter_id")
    private Long reporterId;

    /** 报告人角色 */
    @TableField("reporter_role")
    private String reporterRole;

    /** 报告时间（UTC） */
    @TableField("report_time")
    private LocalDateTime reportTime;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 结果摘要 */
    @TableField("result_summary")
    private String resultSummary;

    /** 解决时间（UTC） */
    @TableField("resolve_time")
    private LocalDateTime resolveTime;
}
