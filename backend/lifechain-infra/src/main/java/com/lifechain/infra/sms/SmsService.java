package com.lifechain.infra.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/** 验证码只有一条正式发送路径；配置缺失、渠道拒绝和网络故障均不能返回发送成功。 */
@Service
@RequiredArgsConstructor
public class SmsService {
    private final ObjectProvider<Client> clients;
    private final SmsProperties properties;
    private final ObjectMapper objectMapper;

    public void sendCode(String mobile, String code) {
        if (!StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getSignName())
                || !StringUtils.hasText(properties.getTemplateCode())) {
            throw new BizException(ErrorCodeEnum.NOTIFICATION_FAILED, "短信服务未配置，请联系平台管理员");
        }
        Client client = clients.getIfAvailable();
        if (client == null) throw new BizException(ErrorCodeEnum.NOTIFICATION_FAILED, "短信服务未配置");
        try {
            var request = new SendSmsRequest().setPhoneNumbers(mobile)
                    .setSignName(properties.getSignName()).setTemplateCode(properties.getTemplateCode())
                    .setTemplateParam(objectMapper.writeValueAsString(Map.of("code", code)));
            // 验证码发送不自动重试，避免请求结果未知时重复计费与重复投递。
            var options = new RuntimeOptions().setConnectTimeout(5_000).setReadTimeout(10_000).setAutoretry(false);
            var response = client.sendSmsWithOptions(request, options);
            if (response == null || response.getBody() == null || !"OK".equals(response.getBody().getCode())) {
                throw new BizException(ErrorCodeEnum.NOTIFICATION_FAILED, "短信渠道未接受发送，请稍后重试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // SDK 异常可能包含请求参数和凭据，只返回稳定的业务提示。
            throw new BizException(ErrorCodeEnum.NOTIFICATION_FAILED, "短信发送失败，请稍后重试");
        }
    }
}
