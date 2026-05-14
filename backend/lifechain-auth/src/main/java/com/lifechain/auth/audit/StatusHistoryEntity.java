package com.lifechain.auth.audit;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 状态变更历史实体
 * <p>
 * 对应数据库表 {@code status_history}，记录所有业务对象的状态流转历史，
 * 包括业务类型、变更前后状态、变更原因、操作人等信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("status_history")
public class StatusHistoryEntity extends BaseEntity {

    /** 业务类型 */
    @TableField("biz_type")
    private String bizType;

    /** 业务ID */
    @TableField("biz_id")
    private Long bizId;

    /** 业务编号 */
    @TableField("biz_no")
    private String bizNo;

    /** 原状态 */
    @TableField("from_status")
    private String fromStatus;

    /** 目标状态 */
    @TableField("to_status")
    private String toStatus;

    /** 变更原因 */
    @TableField("change_reason")
    private String changeReason;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 变更时间（UTC） */
    @TableField("change_time")
    private LocalDateTime changeTime;
}
