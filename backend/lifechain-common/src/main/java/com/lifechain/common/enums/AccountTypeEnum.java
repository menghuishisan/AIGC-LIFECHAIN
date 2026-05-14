package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账户类型枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum AccountTypeEnum {

    /** 个人账户 */
    PERSONAL("PERSONAL", "个人账户"),
    /** 企业账户 */
    ENTERPRISE("ENTERPRISE", "企业账户"),
    /** 平台内部账户 */
    PLATFORM("PLATFORM", "平台内部账户"),
    /** 监管账户 */
    REGULATOR("REGULATOR", "监管账户");

    /** 类型编码 */
    private final String code;
    /** 类型描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 类型编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static AccountTypeEnum fromCode(String code) {
        for (AccountTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的账户类型编码: " + code);
    }
}
