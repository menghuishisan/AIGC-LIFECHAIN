package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 链上状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum ChainStatusEnum {

    /** 待上链 */
    CHAIN_PENDING("CHAIN_PENDING", "待上链"),
    /** 已提交待回执 */
    CHAIN_SUBMITTED("CHAIN_SUBMITTED", "已提交待回执"),
    /** 链上成功 */
    CHAIN_SUCCESS("CHAIN_SUCCESS", "链上成功"),
    /** 链上失败 */
    CHAIN_FAILED("CHAIN_FAILED", "链上失败");

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
    public static ChainStatusEnum fromCode(String code) {
        for (ChainStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的链上状态编码: " + code);
    }
}
