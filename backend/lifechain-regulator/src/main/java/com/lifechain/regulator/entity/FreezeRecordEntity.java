package com.lifechain.regulator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 冻结记录实体
 * <p>
 * 对应数据库表 {@code freeze_record}，记录对账户、作品或订单的冻结/解冻操作，
 * 包含目标信息、冻结模式、审核状态、链上存证等核心字段。
 * 冻结模式分为审核冻结（REVIEW_REQUIRED）和监管直接冻结（REGULATOR_DIRECT）。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("freeze_record")
public class FreezeRecordEntity extends BaseEntity {

    /** 冻结编号（对外唯一标识） */
    @TableField("freeze_no")
    private String freezeNo;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;

    /** 目标类型（ACCOUNT/WORK/ORDER等） */
    @TableField("target_type")
    private String targetType;

    /** 目标ID */
    @TableField("target_id")
    private Long targetId;

    /** 目标编号 */
    @TableField("target_no")
    private String targetNo;

    /** 冻结前目标状态快照 */
    @TableField("previous_target_status")
    private String previousTargetStatus;

    /** 冻结状态 */
    @TableField("freeze_status")
    private String freezeStatus;

    /** 冻结模式（REVIEW_REQUIRED/REGULATOR_DIRECT） */
    @TableField("freeze_mode")
    private String freezeMode;

    /** 审核状态 */
    @TableField("review_status")
    private String reviewStatus;

    /** 冻结原因 */
    @TableField("freeze_reason")
    private String freezeReason;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 申请人ID */
    @TableField("apply_user_id")
    private Long applyUserId;

    /** 申请人角色 */
    @TableField("apply_role")
    private String applyRole;

    /** 申请时间（UTC） */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /** 审批人ID */
    @TableField("approve_user_id")
    private Long approveUserId;

    /** 审批时间（UTC） */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /** 生效时间（UTC） */
    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    /** 解冻原因 */
    @TableField("unfreeze_reason")
    private String unfreezeReason;

    /** 解冻时间（UTC） */
    @TableField("unfreeze_time")
    private LocalDateTime unfreezeTime;

    /** 紧急依据编号 */
    @TableField("urgent_basis_no")
    private String urgentBasisNo;

    /** 链上状态 */
    @TableField("chain_status")
    private String chainStatus;

    /** 交易哈希 */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;
}
