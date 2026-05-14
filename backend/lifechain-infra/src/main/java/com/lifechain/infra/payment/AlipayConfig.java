package com.lifechain.infra.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置
 * <p>
 * 读取 lifechain.pay.alipay 前缀的配置项，包含应用ID、商户私钥、支付宝公钥等。
 * 所有敏感配置建议通过环境变量或加密配置中心注入。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lifechain.pay.alipay")
public class AlipayConfig {

    /** 支付宝应用ID */
    private String appId;

    /** 商户RSA私钥 */
    private String privateKey;

    /** 支付宝RSA公钥 */
    private String alipayPublicKey;

    /** 支付宝网关地址 */
    private String serverUrl;

    /** 支付结果通知URL */
    private String notifyUrl;

    /** 退款结果通知URL */
    private String refundNotifyUrl;
}
