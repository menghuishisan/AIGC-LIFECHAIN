package com.lifechain.auth.audit;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 * <p>
 * 对应数据库表 {@code audit_log}，记录所有关键业务操作的审计日志，
 * 包括操作目标、操作人、操作结果等信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("audit_log")
public class AuditLogEntity extends BaseEntity {

    /** 目标类型 */
    @TableField("target_type")
    private String targetType;

    /** 目标ID */
    @TableField("target_id")
    private Long targetId;

    /** 目标编号 */
    @TableField("target_no")
    private String targetNo;

    /** 操作动作 */
    @TableField("action")
    private String action;

    /** 操作详情 */
    @TableField("action_detail")
    private String actionDetail;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人角色 */
    @TableField("operator_role")
    private String operatorRole;

    /** 操作人IP */
    @TableField("operator_ip")
    private String operatorIp;

    /** 操作结果 */
    @TableField("result")
    private String result;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 日志时间（UTC） */
    @TableField("log_time")
    private LocalDateTime logTime;
}
