package com.lifechain.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 退款申请请求
 * <p>
 * 买方对已支付或已完成的订单申请退款，需说明退款原因。
 * 退款申请提交后需由管理员审批。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class RefundApplyRequest implements Serializable {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /** 退款原因 */
    @NotBlank(message = "退款原因不能为空")
    private String reason;

    /** 幂等请求ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
