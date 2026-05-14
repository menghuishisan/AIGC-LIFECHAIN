package com.lifechain.infra.payment;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 支付适配器接口
 * <p>
 * 定义统一的支付操作契约，各支付渠道（微信、支付宝）分别实现。
 * 上层业务通过 {@link PaymentAdapterFactory} 获取具体适配器，无需关心渠道差异。
 * </p>
 */
public interface PaymentAdapter {

    /**
     * 创建支付订单
     * <p>
     * 调用第三方支付平台下单接口，返回前端调起支付所需的参数。
     * </p>
     *
     * @param request 支付请求参数
     * @return 支付响应（含预付单ID、支付链接、前端参数等）
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 解析微信支付异步回调
     * <p>
     * 验签、解密并解析微信支付通知报文，提取支付结果信息。
     * 非微信支付渠道的适配器实现此方法时应抛出 {@link com.lifechain.common.exception.BizException}。
     * </p>
     *
     * @param request HTTP请求（包含回调头信息和请求体）
     * @return 解析后的回调结果
     */
    PaymentCallbackResult parseWechatCallback(HttpServletRequest request);

    /**
     * 解析支付宝异步回调
     * <p>
     * 验签并解析支付宝通知参数，提取支付结果信息。
     * 非支付宝渠道的适配器实现此方法时应抛出 {@link com.lifechain.common.exception.BizException}。
     * </p>
     *
     * @param request HTTP请求（包含回调表单参数）
     * @return 解析后的回调结果
     */
    PaymentCallbackResult parseAlipayCallback(HttpServletRequest request);

    /**
     * 申请退款
     * <p>
     * 调用第三方支付平台退款接口，支持部分退款和全额退款。
     * </p>
     *
     * @param request 退款请求参数
     * @return 退款响应
     */
    RefundResponse refund(RefundRequest request);
}
