package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 确权申请实体
 * <p>
 * 对应数据库表 {@code claim_application}，存储作品确权申请的完整生命周期信息，
 * 包括申请提交、审核、上链确认等各阶段状态和时间戳，以及链上交易信息。
 * 确权编号（claimNo）为对外唯一标识。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("claim_application")
public class ClaimApplicationEntity extends BaseEntity {

    /** 确权编号（对外唯一标识） */
    @TableField("claim_no")
    private String claimNo;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 申请人账户ID */
    @TableField("applicant_account_id")
    private Long applicantAccountId;

    /** 申请人DID ID */
    @TableField("applicant_did_id")
    private Long applicantDidId;

    /** 确权申请状态 */
    @TableField("status")
    private String status;

    /** 链上状态 */
    @TableField("chain_status")
    private String chainStatus;

    /** 提交时间（UTC） */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /** 审核时间（UTC） */
    @TableField("review_time")
    private LocalDateTime reviewTime;

    /** 审批通过时间（UTC） */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /** 链上提交时间（UTC） */
    @TableField("chain_submit_time")
    private LocalDateTime chainSubmitTime;

    /** 链上确认时间（UTC） */
    @TableField("chain_confirm_time")
    private LocalDateTime chainConfirmTime;

    /** 交易哈希 */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;

    /** 确权摘要哈希 */
    @TableField("summary_hash")
    private String summaryHash;

    /** 审核人ID */
    @TableField("reviewer_id")
    private Long reviewerId;

    /** 审核意见 */
    @TableField("review_comment")
    private String reviewComment;

    /** 拒绝原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;
}
