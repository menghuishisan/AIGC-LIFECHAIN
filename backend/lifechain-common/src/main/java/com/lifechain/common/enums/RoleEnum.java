package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum RoleEnum {

    /** 创作者 */
    CREATOR("CREATOR", "创作者"),
    /** 购买者 */
    BUYER("BUYER", "购买者"),
    /** 平台管理员 */
    PLATFORM_ADMIN("PLATFORM_ADMIN", "平台管理员"),
    /** 监管员 */
    REGULATOR("REGULATOR", "监管员");

    /** 角色编码 */
    private final String code;
    /** 角色描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 角色编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static RoleEnum fromCode(String code) {
        for (RoleEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的角色编码: " + code);
    }
}
