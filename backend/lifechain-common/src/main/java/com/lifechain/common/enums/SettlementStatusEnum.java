package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 结算状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum SettlementStatusEnum {

    /** 未开始 */
    SETTLE_NOT_STARTED("SETTLE_NOT_STARTED", "未开始"),
    /** 待结算 */
    SETTLE_READY("SETTLE_READY", "待结算"),
    /** 结算中 */
    SETTLE_PROCESSING("SETTLE_PROCESSING", "结算中"),
    /** 部分成功 */
    SETTLE_PARTIAL_SUCCESS("SETTLE_PARTIAL_SUCCESS", "部分成功"),
    /** 结算成功 */
    SETTLE_SUCCESS("SETTLE_SUCCESS", "结算成功"),
    /** 结算失败 */
    SETTLE_FAILED("SETTLE_FAILED", "结算失败"),
    /** 逆分账处理中 */
    REVERSE_PENDING("REVERSE_PENDING", "逆分账处理中"),
    /** 逆分账成功 */
    REVERSE_SUCCESS("REVERSE_SUCCESS", "逆分账成功"),
    /** 逆分账失败 */
    REVERSE_FAILED("REVERSE_FAILED", "逆分账失败"),
    /** 结算冻结 */
    SETTLE_FROZEN("SETTLE_FROZEN", "结算冻结");

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
    public static SettlementStatusEnum fromCode(String code) {
        for (SettlementStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的结算状态编码: " + code);
    }
}
