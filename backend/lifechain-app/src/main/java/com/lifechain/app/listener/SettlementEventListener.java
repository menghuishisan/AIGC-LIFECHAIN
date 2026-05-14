package com.lifechain.app.listener;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.OrderStatusEnum;
import com.lifechain.common.event.SettlementCompletedEvent;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 结算事件监听器
 * <p>
 * 监听结算成功事件，将订单状态从 SETTLEMENT_PENDING 推进为 ORDER_COMPLETED。
 * 放在 app 模块以避免 settlement → trade 的循环依赖。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementEventListener {

    private final TradeOrderMapper tradeOrderMapper;
    private final AuditService auditService;

    @EventListener
    public void onSettlementCompleted(SettlementCompletedEvent event) {
        log.info("收到结算成功事件，orderNo={}, settleNo={}", event.getOrderNo(), event.getSettleNo());
        TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(event.getOrderNo());
        if (order == null) {
            log.warn("结算成功事件处理失败，订单不存在：orderNo={}", event.getOrderNo());
            return;
        }
        if (!OrderStatusEnum.SETTLEMENT_PENDING.getCode().equals(order.getOrderStatus())) {
            log.info("订单当前状态不需要推进：orderNo={}, status={}", event.getOrderNo(), order.getOrderStatus());
            return;
        }
        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatusEnum.ORDER_COMPLETED.getCode());
        order.setCompleteTime(com.lifechain.common.util.DateTimeUtil.nowUtc());
        tradeOrderMapper.updateById(order);

        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                fromStatus, OrderStatusEnum.ORDER_COMPLETED.getCode(),
                "结算成功，订单完成", null, null);
        log.info("订单完成：orderNo={}", event.getOrderNo());
    }
}
