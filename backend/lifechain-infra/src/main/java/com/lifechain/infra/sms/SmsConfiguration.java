package com.lifechain.infra.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/** 使用官方 SDK，未配置短信时保留其他业务功能，发送入口明确返回配置错误。 */
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfiguration {
    @Bean
    @Lazy
    @ConditionalOnProperty(prefix = "lifechain.sms", name = "access-key-id")
    public Client smsClient(SmsProperties properties) throws Exception {
        Config config = new Config().setAccessKeyId(properties.getAccessKeyId())
                .setAccessKeySecret(properties.getAccessKeySecret())
                .setEndpoint("dysmsapi.aliyuncs.com");
        return new Client(config);
    }
}
