package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 争议状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum DisputeStatusEnum {

    /** 已提交 */
    DISPUTE_SUBMITTED("DISPUTE_SUBMITTED", "已提交"),
    /** 已受理 */
    DISPUTE_ACCEPTED("DISPUTE_ACCEPTED", "已受理"),
    /** 待补充证据 */
    DISPUTE_EVIDENCE_PENDING("DISPUTE_EVIDENCE_PENDING", "待补充证据"),
    /** 审查中 */
    DISPUTE_REVIEWING("DISPUTE_REVIEWING", "审查中"),
    /** 已解决 */
    DISPUTE_RESOLVED("DISPUTE_RESOLVED", "已解决"),
    /** 解决待上链确认 */
    DISPUTE_RESOLVED_PENDING_CHAIN("DISPUTE_RESOLVED_PENDING_CHAIN", "解决待上链确认"),
    /** 已驳回 */
    DISPUTE_REJECTED("DISPUTE_REJECTED", "已驳回"),
    /** 驳回待上链确认 */
    DISPUTE_REJECTED_PENDING_CHAIN("DISPUTE_REJECTED_PENDING_CHAIN", "驳回待上链确认"),
    /** 已关闭 */
    DISPUTE_CLOSED("DISPUTE_CLOSED", "已关闭"),
    /** 关闭待上链确认 */
    DISPUTE_CLOSED_PENDING_CHAIN("DISPUTE_CLOSED_PENDING_CHAIN", "关闭待上链确认");

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
    public static DisputeStatusEnum fromCode(String code) {
        for (DisputeStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的争议状态编码: " + code);
    }
}
