package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 冻结模式枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum FreezeModeEnum {

    /** 需复核 */
    REVIEW_REQUIRED("REVIEW_REQUIRED", "需复核"),
    /** 监管直接冻结 */
    REGULATOR_DIRECT("REGULATOR_DIRECT", "监管直接冻结");

    /** 模式编码 */
    private final String code;
    /** 模式描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 模式编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static FreezeModeEnum fromCode(String code) {
        for (FreezeModeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的冻结模式编码: " + code);
    }
}
