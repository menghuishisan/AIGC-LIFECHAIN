package com.lifechain.common.enums;

import com.lifechain.common.exception.BizException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 *
 * @author LifeChain
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    /** 已创建 */
    ORDER_CREATED("ORDER_CREATED", "已创建"),
    /** 已过期 */
    ORDER_EXPIRED("ORDER_EXPIRED", "已过期"),
    /** 待支付确认 */
    PAY_PENDING_CONFIRM("PAY_PENDING_CONFIRM", "待支付确认"),
    /** 支付已确认 */
    PAY_CONFIRMED("PAY_CONFIRMED", "支付已确认"),
    /** 授权中 */
    AUTH_GRANTING("AUTH_GRANTING", "授权中"),
    /** 已授权 */
    AUTH_GRANTED("AUTH_GRANTED", "已授权"),
    /** 待结算 */
    SETTLEMENT_PENDING("SETTLEMENT_PENDING", "待结算"),
    /** 已完成 */
    ORDER_COMPLETED("ORDER_COMPLETED", "已完成"),
    /** 退款中 */
    REFUND_PENDING("REFUND_PENDING", "退款中"),
    /** 已退款 */
    REFUNDED("REFUNDED", "已退款"),
    /** 订单冻结 */
    ORDER_FROZEN("ORDER_FROZEN", "订单冻结"),
    /** 已取消 */
    ORDER_CANCELLED("ORDER_CANCELLED", "已取消"),
    /** 异常 */
    ORDER_EXCEPTION("ORDER_EXCEPTION", "异常");

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
    public static OrderStatusEnum fromCode(String code) {
        for (OrderStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "未知的订单状态编码: " + code);
    }
}
