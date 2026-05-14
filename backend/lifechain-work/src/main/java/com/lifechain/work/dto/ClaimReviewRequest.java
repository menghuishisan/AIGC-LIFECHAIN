package com.lifechain.work.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 确权审核请求
 * <p>
 * 管理员对确权申请进行审核时的请求参数，
 * 包括确权编号、审核结果、审核意见和原因码。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ClaimReviewRequest implements Serializable {

    /** 确权编号 */
    @NotBlank(message = "确权编号不能为空")
    private String claimNo;

    /** 审核结果（APPROVED/REJECTED） */
    @NotBlank(message = "审核结果不能为空")
    private String reviewResult;

    /** 审核意见 */
    private String reviewComment;

    /** 原因码（驳回时必填） */
    private String reasonCode;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
