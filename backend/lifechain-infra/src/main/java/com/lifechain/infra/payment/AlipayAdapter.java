package com.lifechain.infra.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.PayChannelEnum;
import com.lifechain.common.exception.BizException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付适配器
 * <p>
 * 基于支付宝开放平台 Java SDK（alipay-sdk-java）实现，使用电脑网站支付（PC扫码）模式。
 * 支持创建支付单、解析回调通知、申请退款等核心操作。
 * 使用RSA2签名方式确保通信安全。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayAdapter implements PaymentAdapter {

    private final AlipayConfig alipayConfig;

    /** 支付宝SDK客户端 */
    private AlipayClient alipayClient;

    /** 支付宝回调时间格式 */
    private static final DateTimeFormatter ALIPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 初始化支付宝SDK客户端
     * <p>
     * 使用RSA2签名方式，JSON数据格式，UTF-8编码。
     * </p>
     */
    @PostConstruct
    public void init() {
        if (alipayConfig.getAppId() == null || alipayConfig.getAppId().isBlank()) {
            log.warn("支付宝配置未设置，跳过初始化");
            return;
        }
        alipayClient = new DefaultAlipayClient(
                alipayConfig.getServerUrl(),
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                "json",
                "UTF-8",
                alipayConfig.getAlipayPublicKey(),
                "RSA2"
        );
        log.info("支付宝适配器初始化完成, 应用ID: {}", alipayConfig.getAppId());
    }

    /**
     * 创建支付宝支付订单（电脑网站支付）
     * <p>
     * 调用 alipay.trade.page.pay 接口，返回支付页面表单HTML。
     * 前端渲染该表单即可跳转至支付宝收银台。
     * 金额从分转换为元（保留2位小数）。
     * </p>
     *
     * @param request 支付请求参数
     * @return 支付响应（含支付表单HTML）
     */
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            AlipayTradePagePayRequest payRequest = new AlipayTradePagePayRequest();
            payRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

            /* 金额单位从分转为元 */
            String amountYuan = convertFenToYuan(request.getTotalAmount());

            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\",\"total_amount\":\"%s\",\"subject\":\"%s\",\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}",
                    request.getOrderNo(),
                    amountYuan,
                    request.getSubject()
            );
            payRequest.setBizContent(bizContent);

            AlipayTradePagePayResponse response = alipayClient.pageExecute(payRequest);

            if (response.isSuccess()) {
                log.info("支付宝下单成功, 订单号: {}", request.getOrderNo());

                Map<String, String> payParams = new HashMap<>();
                payParams.put("formHtml", response.getBody());

                return PaymentResponse.builder()
                        .orderNo(request.getOrderNo())
                        .payUrl(response.getBody())
                        .payChannel(PayChannelEnum.ALIPAY.getCode())
                        .payParams(payParams)
                        .success(true)
                        .build();
            } else {
                log.error("支付宝下单失败, 订单号: {}, 错误码: {}, 错误信息: {}",
                        request.getOrderNo(), response.getSubCode(), response.getSubMsg());
                return PaymentResponse.builder()
                        .orderNo(request.getOrderNo())
                        .payChannel(PayChannelEnum.ALIPAY.getCode())
                        .success(false)
                        .errorMsg("支付宝下单失败: " + response.getSubMsg())
                        .build();
            }

        } catch (AlipayApiException e) {
            log.error("支付宝下单异常, 订单号: {}, 错误: {}", request.getOrderNo(), e.getMessage(), e);
            return PaymentResponse.builder()
                    .orderNo(request.getOrderNo())
                    .payChannel(PayChannelEnum.ALIPAY.getCode())
                    .success(false)
                    .errorMsg("支付宝下单异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析微信支付异步回调（支付宝适配器不支持）
     *
     * @param request HTTP请求
     * @return 不支持，抛出异常
     */
    @Override
    public PaymentCallbackResult parseWechatCallback(HttpServletRequest request) {
        throw new BizException(ErrorCodeEnum.PAY_CHANNEL_UNSUPPORTED,
                "支付宝适配器不支持解析微信支付回调");
    }

    /**
     * 解析支付宝异步回调通知
     * <p>
     * 1. 从请求参数中提取所有回调字段<br>
     * 2. 使用支付宝公钥进行RSA2验签<br>
     * 3. 校验trade_status为TRADE_SUCCESS或TRADE_FINISHED<br>
     * 4. 提取订单号、支付宝流水号、金额、支付时间等信息
     * </p>
     *
     * @param request HTTP请求
     * @return 回调解析结果
     */
    @Override
    public PaymentCallbackResult parseAlipayCallback(HttpServletRequest request) {
        try {
            /* 提取所有回调参数 */
            Map<String, String> params = extractCallbackParams(request);
            String rawPayload = params.toString();

            /* RSA2验签 */
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    "UTF-8",
                    "RSA2"
            );

            if (!signVerified) {
                log.error("支付宝回调验签失败, 参数: {}", rawPayload);
                throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "支付宝回调验签失败");
            }

            String appId = params.get("app_id");
            if (appId == null || !appId.equals(alipayConfig.getAppId())) {
                log.error("支付宝回调 appId 不匹配, expected={}, actual={}", alipayConfig.getAppId(), appId);
                throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "支付宝回调应用 ID 不匹配");
            }

            String tradeStatus = params.get("trade_status");
            boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

            /* 金额从元转为分 */
            Long paidAmount = convertYuanToFen(params.get("total_amount"));

            LocalDateTime payTime = null;
            String gmtPayment = params.get("gmt_payment");
            if (gmtPayment != null && !gmtPayment.isEmpty()) {
                payTime = LocalDateTime.parse(gmtPayment, ALIPAY_DATE_FORMAT);
            }

            log.info("支付宝回调解析成功, 订单号: {}, 交易状态: {}, 流水号: {}",
                    params.get("out_trade_no"), tradeStatus, params.get("trade_no"));

            return PaymentCallbackResult.builder()
                    .orderNo(params.get("out_trade_no"))
                    .thirdTradeNo(params.get("trade_no"))
                    .paidAmount(paidAmount)
                    .payChannel(PayChannelEnum.ALIPAY.getCode())
                    .payTime(payTime)
                    .success(success)
                    .rawPayload(rawPayload)
                    .build();

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝回调解析失败, 错误: {}", e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED,
                    "支付宝回调解析失败: " + e.getMessage());
        }
    }

    /**
     * 申请支付宝退款
     * <p>
     * 调用 alipay.trade.refund 接口，支持部分退款和全额退款。
     * 支付宝退款为同步接口，调用成功即表示退款已受理。
     * </p>
     *
     * @param request 退款请求参数
     * @return 退款响应
     */
    @Override
    public RefundResponse refund(RefundRequest request) {
        try {
            AlipayTradeRefundRequest refundRequest = new AlipayTradeRefundRequest();

            String refundAmountYuan = convertFenToYuan(request.getRefundAmount());

            String bizContent = String.format(
                    "{\"out_trade_no\":\"%s\",\"refund_amount\":\"%s\",\"out_request_no\":\"%s\",\"refund_reason\":\"%s\"}",
                    request.getOrderNo(),
                    refundAmountYuan,
                    request.getRefundNo(),
                    request.getReason() != null ? request.getReason() : "退款"
            );
            refundRequest.setBizContent(bizContent);

            AlipayTradeRefundResponse response = alipayClient.execute(refundRequest);

            if (response.isSuccess()) {
                log.info("支付宝退款成功, 退款单号: {}, 支付宝交易号: {}, 退款金额: {}元",
                        request.getRefundNo(), response.getTradeNo(), refundAmountYuan);

                return RefundResponse.builder()
                        .refundNo(request.getRefundNo())
                        .thirdRefundNo(response.getTradeNo())
                        .success(true)
                        .build();
            } else {
                log.error("支付宝退款失败, 退款单号: {}, 错误码: {}, 错误信息: {}",
                        request.getRefundNo(), response.getSubCode(), response.getSubMsg());

                return RefundResponse.builder()
                        .refundNo(request.getRefundNo())
                        .success(false)
                        .errorMsg("支付宝退款失败: " + response.getSubMsg())
                        .build();
            }

        } catch (AlipayApiException e) {
            log.error("支付宝退款异常, 退款单号: {}, 错误: {}", request.getRefundNo(), e.getMessage(), e);
            return RefundResponse.builder()
                    .refundNo(request.getRefundNo())
                    .success(false)
                    .errorMsg("支付宝退款异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 从HTTP请求中提取支付宝回调参数
     *
     * @param request HTTP请求
     * @return 参数Map
     */
    private Map<String, String> extractCallbackParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
            String[] values = entry.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                    valueStr.append(",");
                }
                valueStr.append(values[i]);
            }
            params.put(entry.getKey(), valueStr.toString());
        }
        return params;
    }

    /**
     * 金额从分转换为元（保留2位小数）
     *
     * @param fen 金额（分）
     * @return 金额字符串（元）
     */
    private String convertFenToYuan(Long fen) {
        return new BigDecimal(fen)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    /**
     * 金额从元转换为分
     *
     * @param yuan 金额字符串（元）
     * @return 金额（分）
     */
    private Long convertYuanToFen(String yuan) {
        return new BigDecimal(yuan)
                .multiply(new BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }
}
