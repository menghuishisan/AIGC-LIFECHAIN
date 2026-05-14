package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审核结果枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum ReviewResultEnum {

    /** 通过 */
    APPROVED("APPROVED", "通过"),
    /** 驳回 */
    REJECTED("REJECTED", "驳回");

    /** 结果编码 */
    private final String code;
    /** 结果描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 结果编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static ReviewResultEnum fromCode(String code) {
        for (ReviewResultEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的审核结果编码: " + code);
    }
}
