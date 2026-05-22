package com.lifechain.app.consumer;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.OrderStatusEnum;
import com.lifechain.common.mq.SettlementCompletedMessage;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 结算完成消息消费者
 * <p>
 * 监听结算成功消息，将关联订单状态从 SETTLEMENT_PENDING 推进为 ORDER_COMPLETED。
 * 放在 app 模块以避免 settlement → trade 的循环依赖。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementCompletedConsumer {

    private final TradeOrderMapper tradeOrderMapper;
    private final AuditService auditService;

    /**
     * 处理结算完成消息
     * <p>
     * 校验订单存在且处于 SETTLEMENT_PENDING 状态后，推进为 ORDER_COMPLETED。
     * </p>
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_SETTLEMENT_COMPLETED)
    public void handle(SettlementCompletedMessage message, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到结算完成消息: orderNo={}, settleNo={}", message.getOrderNo(), message.getSettleNo());
        try {
            // 查询关联订单
            TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(message.getOrderNo());
            if (order == null) {
                log.warn("订单不存在: orderNo={}", message.getOrderNo());
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 状态守卫：仅 SETTLEMENT_PENDING 状态需要推进
            if (!OrderStatusEnum.SETTLEMENT_PENDING.getCode().equals(order.getOrderStatus())) {
                log.info("订单状态无需推进: orderNo={}, status={}", message.getOrderNo(), order.getOrderStatus());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 推进订单状态为完成
            String fromStatus = order.getOrderStatus();
            order.setOrderStatus(OrderStatusEnum.ORDER_COMPLETED.getCode());
            order.setCompleteTime(DateTimeUtil.nowUtc());
            tradeOrderMapper.updateById(order);

            auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                    fromStatus, OrderStatusEnum.ORDER_COMPLETED.getCode(),
                    "结算成功，订单完成", null, null);

            log.info("订单完成: orderNo={}", message.getOrderNo());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理结算完成消息失败: orderNo={}", message.getOrderNo(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
