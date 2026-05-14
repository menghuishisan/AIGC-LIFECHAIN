package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * DID审核请求
 * <p>
 * 管理员对DID申请进行审核，批准后将自动触发上链操作。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class DidReviewRequest implements Serializable {

    /** DID编号 */
    @NotBlank(message = "DID编号不能为空")
    private String didNo;

    /** 审核结果（APPROVED/REJECTED） */
    @NotBlank(message = "审核结果不能为空")
    private String reviewResult;

    /** 审核意见 */
    private String reviewComment;

    /** 原因码 */
    private String reasonCode;

    /** 请求幂等ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
