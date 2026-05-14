package com.lifechain.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 发起支付请求
 * <p>
 * 买方对已创建的订单发起支付，指定支付渠道和客户端IP。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class PayRequest implements Serializable {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderNo;

    /** 支付渠道（WECHAT_PAY/ALIPAY） */
    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    /** 客户端IP */
    private String clientIp;

    /** 幂等请求ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
