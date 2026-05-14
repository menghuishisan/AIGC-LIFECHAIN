package com.lifechain.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 退款处理请求
 * <p>
 * 管理员对退款申请进行审批处理，支持通过（APPROVE）或驳回（REJECT）。
 * 通过后系统自动调用支付渠道退款接口。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class RefundProcessRequest implements Serializable {

    /** 退款编号 */
    @NotBlank(message = "退款编号不能为空")
    private String refundNo;

    /** 处理动作（APPROVE/REJECT） */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    /** 处理备注 */
    private String comment;

    /** 幂等请求ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
