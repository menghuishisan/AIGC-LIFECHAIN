package com.lifechain.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * DID（数字身份）记录实体
 * <p>
 * 对应数据库表 {@code did_record}，存储数字身份的完整生命周期信息，
 * 包括申请、审核、上链、挂起、吊销等各阶段状态和时间戳，以及链上交易信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("did_record")
public class DidRecordEntity extends BaseEntity {

    /** DID编号 */
    @TableField("did_no")
    private String didNo;

    /** DID标识值（W3C DID格式，如 did:lifechain:ACC...） */
    @TableField("did_value")
    private String didValue;

    /** 账户ID */
    @TableField("account_id")
    private Long accountId;

    /** 主体ID */
    @TableField("subject_id")
    private Long subjectId;

    /** DID状态（DID_PENDING/DID_APPROVED_PENDING_CHAIN/DID_ACTIVE/DID_CHAIN_FAILED/DID_SUSPENDED/DID_REVOKED） */
    @TableField("status")
    private String status;

    /** 发起挂起/吊销前的原状态快照 */
    @TableField("previous_status")
    private String previousStatus;

    /** 链上状态（CHAIN_PENDING/CHAIN_SUBMITTED/CHAIN_SUCCESS/CHAIN_FAILED） */
    @TableField("chain_status")
    private String chainStatus;

    /** 申请时间（UTC） */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /** 审批时间（UTC） */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /** 激活时间（UTC） */
    @TableField("active_time")
    private LocalDateTime activeTime;

    /** 挂起时间（UTC） */
    @TableField("suspend_time")
    private LocalDateTime suspendTime;

    /** 吊销时间（UTC） */
    @TableField("revoke_time")
    private LocalDateTime revokeTime;

    /** 交易哈希 */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 审核人ID */
    @TableField("reviewer_id")
    private Long reviewerId;

    /** 审核意见 */
    @TableField("review_comment")
    private String reviewComment;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
