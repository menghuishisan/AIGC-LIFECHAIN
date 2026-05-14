package com.lifechain.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 支付响应DTO
 * <p>
 * 统一封装支付下单后的返回结果。
 * payParams 用于前端调起支付控件，不同渠道所需参数不同。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /** 业务订单编号 */
    private String orderNo;

    /** 预支付交易会话标识（微信）或交易号（支付宝） */
    private String prepayId;

    /** 支付链接（扫码支付时使用） */
    private String payUrl;

    /** 支付渠道 */
    private String payChannel;

    /** 前端调起支付需要的参数（渠道特有） */
    private Map<String, String> payParams;

    /** 是否成功 */
    private boolean success;

    /** 错误信息（失败时填充） */
    private String errorMsg;
}
