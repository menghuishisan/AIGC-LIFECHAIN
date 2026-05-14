package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 实名认证审核请求
 * <p>
 * 管理员对用户实名认证进行审核，可批准或驳回，
 * 批准后自动授予CREATOR角色。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class AuthReviewRequest implements Serializable {

    /** 账户编号 */
    @NotBlank(message = "账户编号不能为空")
    private String accountNo;

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
