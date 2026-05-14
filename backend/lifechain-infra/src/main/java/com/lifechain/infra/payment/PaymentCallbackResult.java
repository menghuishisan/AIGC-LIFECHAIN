package com.lifechain.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付回调结果DTO
 * <p>
 * 统一封装微信支付和支付宝异步回调解析后的结果。
 * rawPayload 保留原始回调报文，便于对账和审计。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackResult {

    /** 业务订单编号 */
    private String orderNo;

    /** 第三方支付流水号 */
    private String thirdTradeNo;

    /** 实际支付金额（单位：分） */
    private Long paidAmount;

    /** 支付渠道 */
    private String payChannel;

    /** 支付完成时间 */
    private LocalDateTime payTime;

    /** 是否成功 */
    private boolean success;

    /** 原始回调报文（JSON或XML字符串，用于对账留痕） */
    private String rawPayload;
}
