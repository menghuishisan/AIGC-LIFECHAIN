package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付渠道枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum PayChannelEnum {

    /** 微信支付 */
    WECHAT_PAY("WECHAT_PAY", "微信支付"),
    /** 支付宝 */
    ALIPAY("ALIPAY", "支付宝"),
    ;

    /** 渠道编码 */
    private final String code;
    /** 渠道描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 渠道编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static PayChannelEnum fromCode(String code) {
        for (PayChannelEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的支付渠道编码: " + code);
    }
}
