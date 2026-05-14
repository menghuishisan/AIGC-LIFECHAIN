package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 风险状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum RiskStatusEnum {

    /** 正常 */
    RISK_NORMAL("RISK_NORMAL", "正常"),
    /** 已标记 */
    RISK_MARKED("RISK_MARKED", "已标记"),
    /** 审查中 */
    RISK_REVIEWING("RISK_REVIEWING", "审查中"),
    /** 已冻结 */
    RISK_FROZEN("RISK_FROZEN", "已冻结"),
    /** 已释放 */
    RISK_RELEASED("RISK_RELEASED", "已释放"),
    /** 已确认 */
    RISK_CONFIRMED("RISK_CONFIRMED", "已确认");

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
    public static RiskStatusEnum fromCode(String code) {
        for (RiskStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的风险状态编码: " + code);
    }
}
