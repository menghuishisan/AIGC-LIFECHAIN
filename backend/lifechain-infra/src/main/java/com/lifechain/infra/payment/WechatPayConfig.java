package com.lifechain.infra.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付配置
 * <p>
 * 读取 lifechain.pay.wechat 前缀的配置项，包含商户号、APIv3密钥、证书路径等。
 * 所有敏感配置建议通过环境变量或加密配置中心注入。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lifechain.pay.wechat")
public class WechatPayConfig {

    /** 微信公众号/小程序AppID */
    private String appId;

    /** 微信商户号 */
    private String mchId;

    /** APIv3密钥（用于回调报文解密） */
    private String apiV3Key;

    /** 商户API证书序列号 */
    private String serialNumber;

    /** 商户API私钥文件路径 */
    private String privateKeyPath;

    /** 微信支付平台证书文件路径 */
    private String certificatePath;

    /** 支付结果通知URL */
    private String notifyUrl;

    /** 退款结果通知URL */
    private String refundNotifyUrl;
}
