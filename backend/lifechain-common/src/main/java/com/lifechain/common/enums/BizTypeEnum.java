package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举（用于审计、存证、链上记录等）
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum BizTypeEnum {

    /** 账户 */
    ACCOUNT("ACCOUNT", "账户"),
    /** 数字身份 */
    DID("DID", "数字身份"),
    /** 作品 */
    WORK("WORK", "作品"),
    /** 确权申请 */
    CLAIM("CLAIM", "确权申请"),
    /** 证书 */
    CERTIFICATE("CERTIFICATE", "证书"),
    /** 订单 */
    ORDER("ORDER", "订单"),
    /** 上架 */
    LISTING("LISTING", "上架"),
    /** 授权 */
    LICENSE("LICENSE", "授权"),
    /** 支付 */
    PAYMENT("PAYMENT", "支付"),
    /** 结算 */
    SETTLEMENT("SETTLEMENT", "结算"),
    /** 逆分账 */
    REVERSE_SETTLEMENT("REVERSE_SETTLEMENT", "逆分账"),
    /** 风险事件 */
    RISK("RISK", "风险事件"),
    /** 冻结 */
    FREEZE("FREEZE", "冻结"),
    /** 争议 */
    DISPUTE("DISPUTE", "争议"),
    /** 监管报告 */
    REPORT("REPORT", "监管报告");

    /** 业务类型编码 */
    private final String code;
    /** 业务类型描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 业务类型编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static BizTypeEnum fromCode(String code) {
        for (BizTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的业务类型编码: " + code);
    }
}
