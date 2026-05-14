package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 冻结记录视图对象
 * <p>
 * 展示冻结记录完整信息，包含目标信息、冻结状态、审核信息、链上存证等。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class FreezeRecordVO implements Serializable {

    /** 冻结编号 */
    private String freezeNo;

    /** 目标类型 */
    private String targetType;

    /** 目标编号 */
    private String targetNo;

    /** 冻结状态 */
    private String freezeStatus;

    /** 冻结模式 */
    private String freezeMode;

    /** 审核状态（PENDING_REVIEW / PENDING_POST_REVIEW / REVIEW_PASSED / REVIEW_REJECTED） */
    private String reviewStatus;

    /** 申请人角色 */
    private String applyRole;

    /** 原因码 */
    private String reasonCode;

    /** 冻结原因 */
    private String freezeReason;

    /** 紧急依据编号 */
    private String urgentBasisNo;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 解冻时间 */
    private LocalDateTime unfreezeTime;

    /** 解冻原因 */
    private String unfreezeReason;

    /** 链上状态 */
    private String chainStatus;

    /** 交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockHeight;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
