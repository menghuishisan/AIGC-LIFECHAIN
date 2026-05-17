package com.lifechain.infra.payment;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.PayChannelEnum;
import com.lifechain.common.exception.BizException;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 微信支付适配器
 * <p>
 * 基于微信支付 Java SDK（wechatpay-java）实现，使用Native支付（扫码支付）模式。
 * 支持创建支付单、解析回调通知、申请退款等核心操作。
 * 回调验签使用自动更新的微信平台证书，无需手动维护。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPayAdapter implements PaymentAdapter {

    private final WechatPayConfig wechatPayConfig;

    /** 微信支付SDK配置（含自动证书更新） */
    private Config config;

    /** Native支付服务 */
    private NativePayService nativePayService;

    /** 退款服务 */
    private RefundService refundService;

    /** 回调通知解析器 */
    private NotificationParser notificationParser;

    /**
     * 初始化微信支付SDK客户端
     * <p>
     * 使用RSA自动证书配置，SDK会自动拉取和更新微信平台证书。
     * </p>
     */
    @PostConstruct
    public void init() {
        if (isBlank(wechatPayConfig.getMchId())
                || isBlank(wechatPayConfig.getAppId())
                || isBlank(wechatPayConfig.getApiV3Key())
                || isBlank(wechatPayConfig.getSerialNumber())
                || isBlank(wechatPayConfig.getPrivateKeyPath())) {
            log.warn("微信支付配置不完整，跳过初始化（缺失必需字段：mchId/appId/apiV3Key/serialNumber/privateKeyPath 之一）");
            return;
        }
        config = new RSAAutoCertificateConfig.Builder()
                .merchantId(wechatPayConfig.getMchId())
                .privateKeyFromPath(wechatPayConfig.getPrivateKeyPath())
                .merchantSerialNumber(wechatPayConfig.getSerialNumber())
                .apiV3Key(wechatPayConfig.getApiV3Key())
                .build();

        nativePayService = new NativePayService.Builder().config(config).build();
        refundService = new RefundService.Builder().config(config).build();
        notificationParser = new NotificationParser((NotificationConfig) config);

        log.info("微信支付适配器初始化完成, 商户号: {}", wechatPayConfig.getMchId());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * 创建微信支付订单（Native扫码支付）
     * <p>
     * 调用微信Native下单接口，返回支付二维码链接。
     * 前端使用该链接生成二维码供用户扫码支付。
     * </p>
     *
     * @param request 支付请求参数
     * @return 支付响应（含支付二维码URL）
     */
    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            PrepayRequest prepayRequest = new PrepayRequest();
            prepayRequest.setAppid(wechatPayConfig.getAppId());
            prepayRequest.setMchid(wechatPayConfig.getMchId());
            prepayRequest.setDescription(request.getSubject());
            prepayRequest.setOutTradeNo(request.getOrderNo());
            prepayRequest.setNotifyUrl(wechatPayConfig.getNotifyUrl());

            Amount amount = new Amount();
            amount.setTotal(request.getTotalAmount().intValue());
            amount.setCurrency("CNY");
            prepayRequest.setAmount(amount);

            PrepayResponse prepayResponse = nativePayService.prepay(prepayRequest);

            log.info("微信支付下单成功, 订单号: {}, codeUrl: {}", request.getOrderNo(), prepayResponse.getCodeUrl());

            Map<String, String> payParams = new HashMap<>();
            payParams.put("codeUrl", prepayResponse.getCodeUrl());

            return PaymentResponse.builder()
                    .orderNo(request.getOrderNo())
                    .payUrl(prepayResponse.getCodeUrl())
                    .payChannel(PayChannelEnum.WECHAT_PAY.getCode())
                    .payParams(payParams)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("微信支付下单失败, 订单号: {}, 错误: {}", request.getOrderNo(), e.getMessage(), e);
            return PaymentResponse.builder()
                    .orderNo(request.getOrderNo())
                    .payChannel(PayChannelEnum.WECHAT_PAY.getCode())
                    .success(false)
                    .errorMsg("微信支付下单失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析微信支付异步回调通知
     * <p>
     * 从HTTP请求头中提取签名信息，使用SDK自动验签并解密通知报文。
     * 提取订单号、支付流水号、支付金额和支付时间等关键信息。
     * </p>
     *
     * @param request HTTP请求
     * @return 回调解析结果
     */
    @Override
    public PaymentCallbackResult parseWechatCallback(HttpServletRequest request) {
        try {
            String serialNumber = request.getHeader("Wechatpay-Serial");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String signature = request.getHeader("Wechatpay-Signature");
            String signatureType = request.getHeader("Wechatpay-Signature-Type");

            String body;
            try (BufferedReader reader = request.getReader()) {
                body = reader.lines().collect(Collectors.joining());
            }

            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNumber)
                    .nonce(nonce)
                    .timestamp(timestamp)
                    .signature(signature)
                    .signType(signatureType != null ? signatureType : "WECHATPAY2-SHA256-RSA2048")
                    .body(body)
                    .build();

            Transaction transaction = notificationParser.parse(requestParam, Transaction.class);

            if (transaction.getMchid() == null || !transaction.getMchid().equals(wechatPayConfig.getMchId())) {
                log.error("微信支付回调商户号不匹配, expected={}, actual={}",
                        wechatPayConfig.getMchId(), transaction.getMchid());
                throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "微信支付回调商户号不匹配");
            }
            if (transaction.getAppid() == null || !transaction.getAppid().equals(wechatPayConfig.getAppId())) {
                log.error("微信支付回调 AppId 不匹配, expected={}, actual={}",
                        wechatPayConfig.getAppId(), transaction.getAppid());
                throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "微信支付回调 AppId 不匹配");
            }

            boolean success = Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState());
            LocalDateTime payTime = null;
            if (transaction.getSuccessTime() != null) {
                payTime = OffsetDateTime.parse(transaction.getSuccessTime()).toLocalDateTime();
            }

            log.info("微信支付回调解析成功, 订单号: {}, 支付状态: {}, 流水号: {}",
                    transaction.getOutTradeNo(), transaction.getTradeState(), transaction.getTransactionId());

            return PaymentCallbackResult.builder()
                    .orderNo(transaction.getOutTradeNo())
                    .thirdTradeNo(transaction.getTransactionId())
                    .paidAmount(transaction.getAmount() != null ? transaction.getAmount().getTotal().longValue() : null)
                    .payChannel(PayChannelEnum.WECHAT_PAY.getCode())
                    .payTime(payTime)
                    .success(success)
                    .rawPayload(body)
                    .build();

        } catch (Exception e) {
            log.error("微信支付回调解析失败, 错误: {}", e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED,
                    "微信支付回调验签或解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析支付宝异步回调（微信适配器不支持）
     *
     * @param request HTTP请求
     * @return 不支持，抛出异常
     */
    @Override
    public PaymentCallbackResult parseAlipayCallback(HttpServletRequest request) {
        throw new BizException(ErrorCodeEnum.PAY_CHANNEL_UNSUPPORTED,
                "微信支付适配器不支持解析支付宝回调");
    }

    /**
     * 申请微信退款
     * <p>
     * 调用微信退款接口，支持部分退款。
     * 退款到账时间取决于微信处理周期，通常1-3个工作日。
     * </p>
     *
     * @param request 退款请求参数
     * @return 退款响应
     */
    @Override
    public RefundResponse refund(RefundRequest request) {
        try {
            CreateRequest createRequest = new CreateRequest();
            createRequest.setOutTradeNo(request.getOrderNo());
            createRequest.setOutRefundNo(request.getRefundNo());
            createRequest.setReason(request.getReason());
            createRequest.setNotifyUrl(wechatPayConfig.getRefundNotifyUrl());

            AmountReq amountReq = new AmountReq();
            amountReq.setRefund(request.getRefundAmount());
            amountReq.setTotal(request.getTotalAmount());
            amountReq.setCurrency("CNY");
            createRequest.setAmount(amountReq);

            Refund refund = refundService.create(createRequest);

            boolean success = Status.SUCCESS.equals(refund.getStatus()) ||
                    Status.PROCESSING.equals(refund.getStatus());

            log.info("微信退款请求完成, 退款单号: {}, 微信退款号: {}, 状态: {}",
                    request.getRefundNo(), refund.getRefundId(), refund.getStatus());

            return RefundResponse.builder()
                    .refundNo(request.getRefundNo())
                    .thirdRefundNo(refund.getRefundId())
                    .success(success)
                    .build();

        } catch (Exception e) {
            log.error("微信退款失败, 退款单号: {}, 错误: {}", request.getRefundNo(), e.getMessage(), e);
            return RefundResponse.builder()
                    .refundNo(request.getRefundNo())
                    .success(false)
                    .errorMsg("微信退款失败: " + e.getMessage())
                    .build();
        }
    }
}
