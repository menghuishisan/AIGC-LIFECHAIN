package com.lifechain.app.consumer;

import com.lifechain.common.mq.OrderExpireMessage;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.lifechain.trade.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.io.IOException;

/** 订单服务提交过期事务后再确认消息；业务失败由定时补偿继续核实，避免消息立即重投循环。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireConsumer {
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_EXPIRE)
    public void handle(OrderExpireMessage message, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            orderService.expireOrder(message.getOrderNo());
        } catch (Exception e) {
            log.warn("订单过期处理待定时补偿: orderNo={}", message.getOrderNo());
        }
        channel.basicAck(deliveryTag, false);
    }
}
