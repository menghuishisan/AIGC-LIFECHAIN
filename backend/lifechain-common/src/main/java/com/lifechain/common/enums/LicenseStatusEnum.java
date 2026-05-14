package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 授权许可状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum LicenseStatusEnum {

    /** 待生效 */
    LICENSE_PENDING("LICENSE_PENDING", "待生效"),
    /** 已生效 */
    LICENSE_ACTIVE("LICENSE_ACTIVE", "已生效"),
    /** 已过期 */
    LICENSE_EXPIRED("LICENSE_EXPIRED", "已过期"),
    /** 已终止 */
    LICENSE_TERMINATED("LICENSE_TERMINATED", "已终止"),
    /** 已冻结 */
    LICENSE_FROZEN("LICENSE_FROZEN", "已冻结"),
    /** 已撤销 */
    LICENSE_REVOKED("LICENSE_REVOKED", "已撤销");

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
    public static LicenseStatusEnum fromCode(String code) {
        for (LicenseStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的授权许可状态编码: " + code);
    }
}
