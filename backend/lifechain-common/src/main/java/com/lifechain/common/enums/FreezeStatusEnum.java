package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 冻结状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum FreezeStatusEnum {

    /** 已发起 */
    FREEZE_APPLIED("FREEZE_APPLIED", "已发起"),
    /** 已批准 */
    FREEZE_APPROVED("FREEZE_APPROVED", "已批准"),
    /** 冻结待上链确认 */
    FREEZE_APPROVED_PENDING_CHAIN("FREEZE_APPROVED_PENDING_CHAIN", "冻结待上链确认"),
    /** 已拒绝 */
    FREEZE_REJECTED("FREEZE_REJECTED", "已拒绝"),
    /** 解冻待上链确认 */
    UNFREEZE_PENDING_CHAIN("UNFREEZE_PENDING_CHAIN", "解冻待上链确认"),
    /** 已发起解冻 */
    UNFREEZE_APPLIED("UNFREEZE_APPLIED", "已发起解冻"),
    /** 已解冻 */
    UNFREEZE_APPROVED("UNFREEZE_APPROVED", "已解冻");

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
    public static FreezeStatusEnum fromCode(String code) {
        for (FreezeStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的冻结状态编码: " + code);
    }
}
