package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 确权申请状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum ClaimStatusEnum {

    /** 草稿 */
    CLAIM_DRAFT("CLAIM_DRAFT", "草稿"),
    /** 已提交 */
    CLAIM_SUBMITTED("CLAIM_SUBMITTED", "已提交"),
    /** 审核中 */
    CLAIM_REVIEWING("CLAIM_REVIEWING", "审核中"),
    /** 已驳回 */
    CLAIM_REJECTED("CLAIM_REJECTED", "已驳回"),
    /** 审核通过待上链 */
    CLAIM_APPROVED_PENDING_CHAIN("CLAIM_APPROVED_PENDING_CHAIN", "审核通过待上链"),
    /** 上链失败 */
    CLAIM_CHAIN_FAILED("CLAIM_CHAIN_FAILED", "上链失败"),
    /** 确权成功 */
    CLAIM_SUCCESS("CLAIM_SUCCESS", "确权成功"),
    /** 已取消 */
    CLAIM_CANCELLED("CLAIM_CANCELLED", "已取消");

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
    public static ClaimStatusEnum fromCode(String code) {
        for (ClaimStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的确权状态编码: " + code);
    }
}
