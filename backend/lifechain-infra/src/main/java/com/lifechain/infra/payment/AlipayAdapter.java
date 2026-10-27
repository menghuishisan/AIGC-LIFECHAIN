package com.lifechain.infra.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.PayChannelEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.util.MoneyUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 支付宝统一收单适配层；表单、验签、交易查询、退款与关单均经官方 SDK。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlipayAdapter implements QueryablePaymentAdapter {
    private static final String SANDBOX_GATEWAY = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String PRODUCTION_GATEWAY = "https://openapi.alipay.com/gateway.do";
    private static final DateTimeFormatter ALIPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AlipayConfig alipayConfig;
    private AlipayClient alipayClient;

    @PostConstruct
    public void init() {
        if (isBlank(alipayConfig.getAppId()) || isBlank(alipayConfig.getServerUrl())
                || isBlank(alipayConfig.getPrivateKey()) || isBlank(alipayConfig.getAlipayPublicKey())
                || isBlank(alipayConfig.getSellerId())) {
            log.warn("支付宝配置不完整，支付功能未启用");
            return;
        }
        if (!Set.of(SANDBOX_GATEWAY, PRODUCTION_GATEWAY).contains(alipayConfig.getServerUrl())) {
            throw new IllegalStateException("支付宝网关必须为受支持的官方 HTTPS 网关");
        }
        try {
            com.alipay.api.AlipayConfig sdk = new com.alipay.api.AlipayConfig();
            sdk.setServerUrl(alipayConfig.getServerUrl());
            sdk.setAppId(alipayConfig.getAppId());
            sdk.setPrivateKey(alipayConfig.getPrivateKey());
            sdk.setAlipayPublicKey(alipayConfig.getAlipayPublicKey());
            sdk.setFormat("json");
            sdk.setCharset("UTF-8");
            sdk.setSignType("RSA2");
            sdk.setConnectTimeout(5000);
            sdk.setReadTimeout(10000);
            alipayClient = new DefaultAlipayClient(sdk);
        } catch (AlipayApiException e) {
            throw new IllegalStateException("支付宝 SDK 配置不可用");
        }
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        requireClient();
        validateAmount(request.getTotalAmount());
        validateReturnUrl();
        try {
            AlipayTradePagePayRequest payRequest = new AlipayTradePagePayRequest();
            String notifyUrl = alipayConfig.getNotifyUrl();
            if (isPublicHttps(notifyUrl)) {
                payRequest.setNotifyUrl(notifyUrl);
            } else if (PRODUCTION_GATEWAY.equals(alipayConfig.getServerUrl())) {
                throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, "正式支付需要公网 HTTPS 通知地址");
            }
            payRequest.setReturnUrl(alipayConfig.getReturnUrl());
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(request.getOrderNo());
            model.setTotalAmount(convertFenToYuan(request.getTotalAmount()));
            model.setSubject(request.getSubject());
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            if (request.getExpireTime() == null) {
                throw new BizException(ErrorCodeEnum.PARAM_INVALID, "支付订单缺少过期时间");
            }
            model.setTimeExpire(request.getExpireTime().atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(ZoneId.of("Asia/Shanghai")).format(ALIPAY_DATE_FORMAT));
            payRequest.setBizModel(model);
            var response = alipayClient.pageExecute(payRequest, "POST");
            if (!response.isSuccess() || isBlank(response.getBody())) {
                throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, "无法生成支付宝收银台表单");
            }
            // HTML 只存放在专用字段，避免被前端当作 URL 打开或输出到日志。
            return PaymentResponse.builder().orderNo(request.getOrderNo())
                    .payChannel(PayChannelEnum.ALIPAY.getCode())
                    .formHtml(response.getBody()).success(true).build();
        } catch (AlipayApiException e) {
            log.warn("支付宝表单生成失败: orderNo={}", request.getOrderNo());
            throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, "支付宝支付暂不可用");
        }
    }

    @Override
    public PaymentQueryResult queryPayment(String orderNo) {
        requireClient();
        try {
            var request = new AlipayTradeQueryRequest();
            var model = new AlipayTradeQueryModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);
            var response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                String state = "ACQ.TRADE_NOT_EXIST".equals(response.getSubCode()) ? "NOT_FOUND" : "UNKNOWN";
                return PaymentQueryResult.builder().orderNo(orderNo).tradeStatus(state).build();
            }
            if (!orderNo.equals(response.getOutTradeNo())) {
                throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "支付宝查询订单不匹配");
            }
            return PaymentQueryResult.builder().orderNo(orderNo).thirdTradeNo(response.getTradeNo())
                    .tradeStatus(response.getTradeStatus()).totalAmount(convertYuanToFen(response.getTotalAmount()))
                    .payTime(response.getSendPayDate() == null ? null
                            : LocalDateTime.ofInstant(response.getSendPayDate().toInstant(), ZoneOffset.UTC)).build();
        } catch (AlipayApiException e) {
            log.warn("支付宝查询结果未知: orderNo={}", orderNo);
            return PaymentQueryResult.builder().orderNo(orderNo).tradeStatus("UNKNOWN").build();
        }
    }

    @Override
    public void closePayment(String orderNo) {
        requireClient();
        try {
            var request = new AlipayTradeCloseRequest();
            var model = new AlipayTradeCloseModel();
            model.setOutTradeNo(orderNo);
            request.setBizModel(model);
            var response = alipayClient.execute(request);
            if (!response.isSuccess() && !"ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())
                    && !"ACQ.TRADE_HAS_CLOSE".equals(response.getSubCode())) {
                throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "支付宝关单未确认，请先刷新支付状态");
            }
        } catch (AlipayApiException e) {
            throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "支付宝关单结果未知，请稍后查询");
        }
    }

    @Override
    public PaymentCallbackResult parseWechatCallback(HttpServletRequest request) {
        throw new BizException(ErrorCodeEnum.PAY_CHANNEL_UNSUPPORTED);
    }

    @Override
    public PaymentCallbackResult parseAlipayCallback(HttpServletRequest request) {
        requireClient();
        try {
            Map<String, String> params = new HashMap<>();
            for (var entry : request.getParameterMap().entrySet()) {
                if (entry.getValue() == null || entry.getValue().length != 1) {
                    throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "支付宝通知存在重复参数");
                }
                params.put(entry.getKey(), entry.getValue()[0]);
            }
            if (!alipayConfig.getAppId().equals(params.get("app_id"))
                    || !"RSA2".equals(params.get("sign_type"))
                    || !AlipaySignature.rsaCheckV1(params, alipayConfig.getAlipayPublicKey(), "UTF-8", "RSA2")) {
                throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "支付宝通知验签失败");
            }
            if (!alipayConfig.getSellerId().equals(params.get("seller_id"))) {
                throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "支付宝收款商户不匹配");
            }
            for (String field : Set.of("out_trade_no", "trade_no", "notify_id", "trade_status", "total_amount")) {
                if (isBlank(params.get(field))) {
                    throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "支付宝通知缺少必要字段");
                }
            }
            String status = params.get("trade_status");
            boolean paid = "TRADE_SUCCESS".equals(status) || "TRADE_FINISHED".equals(status);
            boolean otherEvent = !paid || !isBlank(params.get("out_biz_no"))
                    || !isBlank(params.get("gmt_refund")) || !isBlank(params.get("refund_fee"));
            LocalDateTime payTime = isBlank(params.get("gmt_payment")) ? null
                    : LocalDateTime.parse(params.get("gmt_payment"), ALIPAY_DATE_FORMAT)
                    .atZone(ZoneId.of("Asia/Shanghai")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            // 保存去重所需业务元数据的摘要来源，不保留签名、买家身份或完整通知正文。
            String auditSource = params.get("notify_id") + ":" + params.get("out_trade_no") + ":"
                    + params.get("trade_no") + ":" + status + ":" + params.get("total_amount");
            return PaymentCallbackResult.builder().orderNo(params.get("out_trade_no"))
                    .thirdTradeNo(params.get("trade_no")).paidAmount(convertYuanToFen(params.get("total_amount")))
                    .payChannel(PayChannelEnum.ALIPAY.getCode()).notifyId(params.get("notify_id"))
                    .tradeStatus(status).nonPaymentEvent(otherEvent).payTime(payTime)
                    .success(paid && !otherEvent).rawPayload(auditSource).build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("支付宝通知解析或验签失败");
            throw new BizException(ErrorCodeEnum.PAY_SIGN_VERIFY_FAILED, "支付宝通知校验失败");
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        requireClient();
        validateAmount(request.getRefundAmount());
        if (request.getTotalAmount() == null || request.getRefundAmount() > request.getTotalAmount()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "退款金额超过原支付金额");
        }
        try {
            var sdkRequest = new AlipayTradeRefundRequest();
            var model = new AlipayTradeRefundModel();
            model.setOutTradeNo(request.getOrderNo());
            model.setRefundAmount(convertFenToYuan(request.getRefundAmount()));
            model.setOutRequestNo(request.getRefundNo());
            model.setRefundReason(request.getReason());
            sdkRequest.setBizModel(model);
            var response = alipayClient.execute(sdkRequest);
            if (!response.isSuccess()) {
                if ("20000".equals(response.getCode())) {
                    return pendingRefund(request.getRefundNo());
                }
                return RefundResponse.builder().refundNo(request.getRefundNo()).success(false)
                        .errorMsg("支付宝退款未受理，请核实原退款单").build();
            }
            if (!request.getOrderNo().equals(response.getOutTradeNo())
                    || !request.getThirdTradeNo().equals(response.getTradeNo())
                    || ("Y".equals(response.getFundChange())
                        && !request.getRefundAmount().equals(convertYuanToFen(response.getRefundFee())))) {
                throw new BizException(ErrorCodeEnum.REFUND_FAILED, "支付宝退款结果与原交易不匹配");
            }
            return RefundResponse.builder().refundNo(request.getRefundNo()).thirdRefundNo(response.getTradeNo())
                    .success(true).pending(!"Y".equals(response.getFundChange())).build();
        } catch (AlipayApiException e) {
            // 网络异常可能已产生资金变动，保留原退款编号并交由查询确认。
            return pendingRefund(request.getRefundNo());
        }
    }

    @Override
    public RefundResponse queryRefund(String orderNo, String refundNo, Long refundAmount) {
        requireClient();
        try {
            var request = new AlipayTradeFastpayRefundQueryRequest();
            var model = new AlipayTradeFastpayRefundQueryModel();
            model.setOutTradeNo(orderNo);
            model.setOutRequestNo(refundNo);
            request.setBizModel(model);
            var response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                return pendingRefund(refundNo);
            }
            if (!orderNo.equals(response.getOutTradeNo()) || !refundNo.equals(response.getOutRequestNo())) {
                throw new BizException(ErrorCodeEnum.REFUND_FAILED, "支付宝退款查询标识不匹配");
            }
            boolean confirmed = "REFUND_SUCCESS".equals(response.getRefundStatus());
            if (confirmed && !refundAmount.equals(convertYuanToFen(response.getRefundAmount()))) {
                throw new BizException(ErrorCodeEnum.REFUND_FAILED, "支付宝退款查询金额不匹配");
            }
            return RefundResponse.builder().refundNo(refundNo).thirdRefundNo(response.getTradeNo())
                    .success(true).pending(!confirmed).build();
        } catch (AlipayApiException e) {
            return pendingRefund(refundNo);
        }
    }

    private RefundResponse pendingRefund(String refundNo) {
        return RefundResponse.builder().refundNo(refundNo).success(true).pending(true).build();
    }

    private void requireClient() {
        if (alipayClient == null) {
            throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, "支付宝尚未配置完整，请完成应用与收款商户配置");
        }
    }

    private void validateReturnUrl() {
        try {
            URI uri = URI.create(alipayConfig.getReturnUrl());
            boolean loopback = Set.of("localhost", "127.0.0.1", "::1").contains(uri.getHost());
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                    || !("https".equals(uri.getScheme()) || (loopback && "http".equals(uri.getScheme())))
                    || (PRODUCTION_GATEWAY.equals(alipayConfig.getServerUrl()) && !isPublicHttps(uri.toString()))) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, "支付宝支付结果页地址未正确配置");
        }
    }

    private static boolean isPublicHttps(String value) {
        if (isBlank(value)) return false;
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getFragment() == null && !Set.of("localhost", "127.0.0.1", "::1").contains(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void validateAmount(Long amount) {
        MoneyUtil.requireFen(amount);
    }

    private static String convertFenToYuan(Long fen) {
        return MoneyUtil.fenToYuan(fen);
    }

    private static Long convertYuanToFen(String yuan) {
        return MoneyUtil.yuanToFen(yuan);
    }
}
