package com.lifechain.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款请求DTO
 * <p>
 * 统一封装微信支付和支付宝的退款请求参数。
 * 金额单位为分，需同时传入原交易金额和退款金额。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    /** 退款单号（业务侧生成） */
    private String refundNo;

    /** 原业务订单编号 */
    private String orderNo;

    /** 原支付流水号（第三方） */
    private String thirdTradeNo;

    /** 退款金额（单位：分） */
    private Long refundAmount;

    /** 原订单总金额（单位：分） */
    private Long totalAmount;

    /** 支付渠道：WECHAT_PAY / ALIPAY */
    private String payChannel;

    /** 退款原因 */
    private String reason;

    /** 退款结果异步通知URL */
    private String notifyUrl;
}
