package com.lifechain.infra.sms;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 阿里云短信签名、验证码模板与服务端凭据，禁止将凭据写入日志。 */
@Data
@ConfigurationProperties(prefix = "lifechain.sms")
public class SmsProperties {
    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;
}
