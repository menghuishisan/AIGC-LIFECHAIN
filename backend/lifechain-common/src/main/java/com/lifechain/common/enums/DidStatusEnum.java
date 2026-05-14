package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数字身份（DID）状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum DidStatusEnum {

    /** 未申请 */
    DID_NOT_APPLIED("DID_NOT_APPLIED", "未申请"),
    /** 审核中 */
    DID_PENDING("DID_PENDING", "审核中"),
    /** 审核通过待上链 */
    DID_APPROVED_PENDING_CHAIN("DID_APPROVED_PENDING_CHAIN", "审核通过待上链"),
    /** 已生效 */
    DID_ACTIVE("DID_ACTIVE", "已生效"),
    /** 上链失败 */
    DID_CHAIN_FAILED("DID_CHAIN_FAILED", "上链失败"),
    /** 挂起待上链确认 */
    DID_SUSPEND_PENDING_CHAIN("DID_SUSPEND_PENDING_CHAIN", "挂起待上链确认"),
    /** 已暂停 */
    DID_SUSPENDED("DID_SUSPENDED", "已暂停"),
    /** 吊销待上链确认 */
    DID_REVOKE_PENDING_CHAIN("DID_REVOKE_PENDING_CHAIN", "吊销待上链确认"),
    /** 已撤销 */
    DID_REVOKED("DID_REVOKED", "已撤销");

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
    public static DidStatusEnum fromCode(String code) {
        for (DidStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的DID状态编码: " + code);
    }
}
