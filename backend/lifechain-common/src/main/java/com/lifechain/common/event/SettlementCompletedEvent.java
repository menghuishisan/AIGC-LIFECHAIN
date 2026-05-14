package com.lifechain.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 结算成功事件
 * <p>
 * 结算链上回执确认成功后发布此事件，用于跨模块通知订单更新状态为 ORDER_COMPLETED。
 * </p>
 */
@Getter
public class SettlementCompletedEvent extends ApplicationEvent {

    private final String orderNo;
    private final Long orderId;
    private final String settleNo;

    public SettlementCompletedEvent(Object source, String orderNo, Long orderId, String settleNo) {
        super(source);
        this.orderNo = orderNo;
        this.orderId = orderId;
        this.settleNo = settleNo;
    }
}
