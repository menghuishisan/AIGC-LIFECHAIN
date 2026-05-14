package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 账户状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum AccountStatusEnum {

    /** 已注册 */
    REGISTERED("REGISTERED", "已注册"),
    /** 认证审核中 */
    AUTH_PENDING("AUTH_PENDING", "认证审核中"),
    /** 认证驳回 */
    AUTH_REJECTED("AUTH_REJECTED", "认证驳回"),
    /** 认证通过 */
    AUTH_APPROVED("AUTH_APPROVED", "认证通过"),
    /** 账户已冻结 */
    ACCOUNT_FROZEN("ACCOUNT_FROZEN", "账户已冻结"),
    /** 账户已停用 */
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账户已停用");

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
    public static AccountStatusEnum fromCode(String code) {
        for (AccountStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的账户状态编码: " + code);
    }
}
