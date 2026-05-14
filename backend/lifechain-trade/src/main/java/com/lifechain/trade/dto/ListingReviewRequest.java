package com.lifechain.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 上架审核请求
 * <p>
 * 管理员审核上架申请时提交的请求参数。
 * 审核结果为 APPROVED（通过）或 REJECTED（驳回）。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ListingReviewRequest implements Serializable {

    /** 上架编号 */
    @NotBlank(message = "上架编号不能为空")
    private String listingNo;

    /** 审核结果（APPROVED/REJECTED） */
    @NotBlank(message = "审核结果不能为空")
    private String reviewResult;

    /** 审核意见 */
    private String reviewComment;

    /** 幂等请求ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
