package com.lifechain.app.consumer;

import com.lifechain.common.mq.SettlementCompletedMessage;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.lifechain.trade.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** 结算完成先在订单事务内验证并提交，再确认消息，防止与退款并发覆盖状态。 */
@Component
@RequiredArgsConstructor
public class SettlementCompletedConsumer {
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SETTLEMENT_COMPLETED)
    public void handle(SettlementCompletedMessage message, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            orderService.completeSettlement(message.getOrderNo());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
