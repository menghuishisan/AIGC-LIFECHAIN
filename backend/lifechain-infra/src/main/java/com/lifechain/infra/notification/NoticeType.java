package com.lifechain.infra.notification;

import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 * <p>
 * 定义系统中所有通知消息的类型分类，用于消息推送和查询过滤。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum NoticeType {

    /** 系统通知 */
    SYSTEM("SYSTEM", "系统通知"),

    /** 确权结果通知 */
    CLAIM_RESULT("CLAIM_RESULT", "确权结果通知"),

    /** 订单状态变更通知 */
    ORDER_STATUS("ORDER_STATUS", "订单状态变更通知"),

    /** 支付相关通知 */
    PAYMENT("PAYMENT", "支付通知"),

    /** 争议相关通知 */
    DISPUTE("DISPUTE", "争议通知"),

    /** 风险预警通知 */
    RISK("RISK", "风险预警通知"),

    /** 结算相关通知 */
    SETTLEMENT("SETTLEMENT", "结算通知");

    /** 通知类型编码 */
    private final String code;

    /** 通知类型描述 */
    private final String description;

    /**
     * 根据编码获取枚举值
     *
     * @param code 通知类型编码
     * @return 对应的枚举值
     * @throws IllegalArgumentException 编码不存在时抛出
     */
    public static NoticeType fromCode(String code) {
        for (NoticeType value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的通知类型编码: " + code);
    }
}
