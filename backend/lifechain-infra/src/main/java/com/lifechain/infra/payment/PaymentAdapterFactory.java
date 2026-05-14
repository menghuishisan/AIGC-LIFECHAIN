package com.lifechain.infra.payment;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.PayChannelEnum;
import com.lifechain.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付适配器工厂
 * <p>
 * 根据支付渠道选择对应的支付适配器，对上层业务屏蔽渠道差异。
 * 提供统一的下单和退款入口，自动路由到微信、支付宝适配器。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAdapterFactory {

    private final WechatPayAdapter wechatPayAdapter;
    private final AlipayAdapter alipayAdapter;

    /**
     * 根据支付渠道获取对应的支付适配器
     *
     * @param payChannel 支付渠道编码（WECHAT_PAY / ALIPAY）
     * @return 对应的支付适配器实例
     * @throws BizException 不支持的支付渠道
     */
    public PaymentAdapter getAdapter(String payChannel) {
        PayChannelEnum channel = PayChannelEnum.fromCode(payChannel);
        return switch (channel) {
            case WECHAT_PAY -> wechatPayAdapter;
            case ALIPAY -> alipayAdapter;
        };
    }

    /**
     * 创建支付订单（自动路由到对应渠道）
     *
     * @param request 支付请求参数（需包含payChannel字段）
     * @return 支付响应
     * @throws BizException 不支持的支付渠道或下单失败
     */
    public PaymentResponse createPayment(PaymentRequest request) {
        log.info("创建支付订单, 订单号: {}, 渠道: {}, 金额: {}分",
                request.getOrderNo(), request.getPayChannel(), request.getTotalAmount());
        PaymentAdapter adapter = getAdapter(request.getPayChannel());
        PaymentResponse response = adapter.createPayment(request);
        if (!response.isSuccess()) {
            log.error("支付下单失败, 订单号: {}, 错误: {}", request.getOrderNo(), response.getErrorMsg());
            throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, response.getErrorMsg());
        }
        return response;
    }

    /**
     * 申请退款（自动路由到对应渠道）
     *
     * @param request 退款请求参数（需包含payChannel字段）
     * @return 退款响应
     * @throws BizException 不支持的支付渠道或退款失败
     */
    public RefundResponse refund(RefundRequest request) {
        log.info("申请退款, 退款单号: {}, 渠道: {}, 金额: {}分",
                request.getRefundNo(), request.getPayChannel(), request.getRefundAmount());
        PaymentAdapter adapter = getAdapter(request.getPayChannel());
        RefundResponse response = adapter.refund(request);
        if (!response.isSuccess()) {
            log.error("退款失败, 退款单号: {}, 错误: {}", request.getRefundNo(), response.getErrorMsg());
            throw new BizException(ErrorCodeEnum.REFUND_FAILED, response.getErrorMsg());
        }
        return response;
    }
}
