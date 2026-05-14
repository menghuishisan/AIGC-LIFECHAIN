package com.lifechain.trade.controller;

import com.lifechain.infra.payment.PaymentAdapter;
import com.lifechain.infra.payment.PaymentAdapterFactory;
import com.lifechain.infra.payment.PaymentCallbackResult;
import com.lifechain.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付回调控制器。
 * <p>
 * 接收微信、支付宝的异步支付结果通知。
 * 验签与报文解析由支付适配器负责，业务层只处理订单状态更新。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Hidden
public class PaymentCallbackController {

    private final OrderService orderService;
    private final PaymentAdapterFactory paymentAdapterFactory;

    /**
     * 处理微信支付异步回调。
     *
     * @param request HTTP 请求
     * @return 微信要求的 XML 响应
     */
    @PostMapping(value = "/wechat/callback", produces = MediaType.APPLICATION_XML_VALUE)
    public String wechatCallback(HttpServletRequest request) {
        log.info("收到微信支付回调通知");
        try {
            PaymentAdapter adapter = paymentAdapterFactory.getAdapter("WECHAT_PAY");
            PaymentCallbackResult result = adapter.parseWechatCallback(request);
            orderService.handlePaymentCallback("WECHAT_PAY", result);
            log.info("微信支付回调处理完成: orderNo={}", result.getOrderNo());
        } catch (Exception e) {
            log.error("微信支付回调处理异常", e);
            return "<xml><return_code><![CDATA[FAIL]]></return_code>"
                    + "<return_msg><![CDATA[处理失败]]></return_msg></xml>";
        }
        return "<xml><return_code><![CDATA[SUCCESS]]></return_code>"
                + "<return_msg><![CDATA[OK]]></return_msg></xml>";
    }

    /**
     * 处理支付宝异步回调。
     *
     * @param request HTTP 请求
     * @return 支付宝要求的文本响应
     */
    @PostMapping(value = "/alipay/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝回调通知");
        try {
            PaymentAdapter adapter = paymentAdapterFactory.getAdapter("ALIPAY");
            PaymentCallbackResult result = adapter.parseAlipayCallback(request);
            orderService.handlePaymentCallback("ALIPAY", result);
            log.info("支付宝回调处理完成: orderNo={}", result.getOrderNo());
        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return "failure";
        }
        return "success";
    }
}
