package com.lifechain.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付请求DTO
 * <p>
 * 统一封装微信支付、支付宝的创建支付请求参数。
 * 金额单位为分（最小货币单位），由调用方自行转换。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    /** 业务订单编号 */
    private String orderNo;

    /** 商品描述 */
    private String subject;

    /** 支付金额（单位：分） */
    private Long totalAmount;

    /** 支付渠道：WECHAT_PAY / ALIPAY */
    private String payChannel;

    /** 支付结果异步通知URL */
    private String notifyUrl;

    /** 客户端IP */
    private String clientIp;

    /** 幂等请求ID */
    private String requestId;
}
