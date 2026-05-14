package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 目标类型枚举（用于冻结、风险等操作的目标对象类型）
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum TargetTypeEnum {

    /** 账户 */
    ACCOUNT("ACCOUNT", "账户"),
    /** 作品 */
    WORK("WORK", "作品"),
    /** 订单 */
    ORDER("ORDER", "订单"),
    /** 授权 */
    LICENSE("LICENSE", "授权");

    /** 目标类型编码 */
    private final String code;
    /** 目标类型描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 目标类型编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static TargetTypeEnum fromCode(String code) {
        for (TargetTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的目标类型编码: " + code);
    }
}
