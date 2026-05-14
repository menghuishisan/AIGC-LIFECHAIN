package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum {

    /** 待创建支付单 */
    PAY_INIT("PAY_INIT", "待创建支付单"),
    /** 待支付确认 */
    PAY_PENDING("PAY_PENDING", "待支付确认"),
    /** 支付成功 */
    PAY_SUCCESS("PAY_SUCCESS", "支付成功"),
    /** 支付失败 */
    PAY_FAILED("PAY_FAILED", "支付失败"),
    /** 支付关闭 */
    PAY_CLOSED("PAY_CLOSED", "支付关闭");

    /** 状态编码 */
    private final String code;
    /** 状态描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 状态编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static PayStatusEnum fromCode(String code) {
        for (PayStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的支付状态编码: " + code);
    }
}
