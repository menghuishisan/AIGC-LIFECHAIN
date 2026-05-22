package com.lifechain.app.consumer;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.OrderStatusEnum;
import com.lifechain.common.enums.PayStatusEnum;
import com.lifechain.common.mq.OrderExpireMessage;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.lifechain.trade.entity.PaymentRecordEntity;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.PaymentRecordMapper;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * 订单过期消息消费者
 * <p>
 * 消费延迟队列中到期的订单过期消息，将超时未支付的订单状态变更为 ORDER_EXPIRED，
 * 并关闭关联的待支付记录。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireConsumer {

    private final TradeOrderMapper orderMapper;
    private final PaymentRecordMapper paymentMapper;
    private final AuditService auditService;

    /**
     * 处理订单过期消息
     * <p>
     * 从延迟队列接收到期消息后，校验订单状态并执行过期操作，
     * 关闭关联支付记录，写入审计日志。
     * </p>
     *
     * @param message     订单过期消息体，包含 orderId 和 orderNo
     * @param channel     RabbitMQ 通道，用于手动 ACK/NACK
     * @param deliveryTag 消息投递标签
     * @throws IOException 通道 ACK/NACK 操作异常
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_EXPIRE)
    @Transactional(rollbackFor = Exception.class)
    public void handle(OrderExpireMessage message, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到订单过期消息: orderNo={}", message.getOrderNo());
        try {
            // 查询订单记录
            TradeOrderEntity order = orderMapper.selectByOrderNo(message.getOrderNo());
            if (order == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 状态守卫：仅处理 ORDER_CREATED 状态的订单，其他状态说明已被处理
            if (!OrderStatusEnum.ORDER_CREATED.getCode().equals(order.getOrderStatus())) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 将订单标记为过期
            String fromStatus = order.getOrderStatus();
            order.setOrderStatus(OrderStatusEnum.ORDER_EXPIRED.getCode());
            order.setCancelTime(DateTimeUtil.nowUtc());
            order.setCancelReason("订单超时自动过期");
            orderMapper.updateById(order);

            // 关闭关联的待支付/初始化状态的支付记录
            List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
            for (PaymentRecordEntity payment : payments) {
                if (PayStatusEnum.PAY_PENDING.getCode().equals(payment.getPayStatus())
                        || PayStatusEnum.PAY_INIT.getCode().equals(payment.getPayStatus())) {
                    payment.setPayStatus(PayStatusEnum.PAY_CLOSED.getCode());
                    paymentMapper.updateById(payment);
                }
            }

            // 写入状态变更审计日志
            auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                    fromStatus, OrderStatusEnum.ORDER_EXPIRED.getCode(),
                    "订单超时自动过期", "ORDER_EXPIRED", null);

            log.info("订单已过期: orderNo={}", message.getOrderNo());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理订单过期消息失败: orderNo={}", message.getOrderNo(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
