package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 确权审核记录实体
 * <p>
 * 对应数据库表 {@code claim_review_record}，存储确权申请的每一次审核记录，
 * 包括审核人、审核动作、审核结果、审核意见及原因码。
 * 同一确权申请可能经历多次审核（如驳回后重新提交）。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("claim_review_record")
public class ClaimReviewRecordEntity extends BaseEntity {

    /** 确权申请ID */
    @TableField("claim_id")
    private Long claimId;

    /** 审核人ID */
    @TableField("reviewer_id")
    private Long reviewerId;

    /** 审核动作 */
    @TableField("review_action")
    private String reviewAction;

    /** 审核结果（APPROVED/REJECTED） */
    @TableField("review_result")
    private String reviewResult;

    /** 审核意见 */
    @TableField("review_comment")
    private String reviewComment;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 审核时间（UTC） */
    @TableField("review_time")
    private LocalDateTime reviewTime;
}
