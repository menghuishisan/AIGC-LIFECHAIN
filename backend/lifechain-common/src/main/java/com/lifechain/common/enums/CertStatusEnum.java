package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 证书状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum CertStatusEnum {

    /** 待生成 */
    CERT_PENDING("CERT_PENDING", "待生成"),
    /** 生成中 */
    CERT_GENERATING("CERT_GENERATING", "生成中"),
    /** 有效 */
    CERT_ACTIVE("CERT_ACTIVE", "有效"),
    /** 失效 */
    CERT_INVALID("CERT_INVALID", "失效"),
    /** 已撤销 */
    CERT_REVOKED("CERT_REVOKED", "已撤销"),
    /** 重新生成中 */
    CERT_REGENERATING("CERT_REGENERATING", "重新生成中");

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
    public static CertStatusEnum fromCode(String code) {
        for (CertStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的证书状态编码: " + code);
    }
}
