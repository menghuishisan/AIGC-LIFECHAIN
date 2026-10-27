package com.lifechain.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 退款记录状态；与订单的 REFUND_PENDING 状态分别建模。 */
@Getter
@AllArgsConstructor
public enum RefundStatusEnum {
    PENDING("PENDING", "待审批"),
    PROCESSING("PROCESSING", "退款结果待确认"),
    SUCCESS("SUCCESS", "退款成功"),
    FAILED("FAILED", "退款失败"),
    REJECTED("REJECTED", "已拒绝");

    private final String code;
    private final String description;
}
