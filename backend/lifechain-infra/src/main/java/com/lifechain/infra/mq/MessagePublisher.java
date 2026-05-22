package com.lifechain.infra.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 统一消息发布器
 * <p>
 * 封装 RabbitMQ 消息发送操作，支持即时消息和延迟消息（TTL + DLX 方式）。
 * 所有业务模块通过此组件发送异步消息，不直接操作 RabbitTemplate。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送即时消息到业务交换机
     *
     * @param routingKey 路由键
     * @param message    消息体（自动序列化为 JSON）
     */
    public void send(String routingKey, Object message) {
        log.info("发送消息: routingKey={}, message={}", routingKey, message);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_LIFECHAIN, routingKey, message);
    }

    /**
     * 发送延迟消息（通过 per-message TTL + 死信队列实现）
     * <p>
     * 消息先进入延迟队列，TTL 到期后由死信交换机转发到实际消费队列。
     * </p>
     *
     * @param delayRoutingKey 延迟队列的路由键（如 order.expire.delay）
     * @param message         消息体
     * @param delayMillis     延迟时间（毫秒）
     */
    public void sendDelayed(String delayRoutingKey, Object message, long delayMillis) {
        log.info("发送延迟消息: routingKey={}, delay={}ms, message={}", delayRoutingKey, delayMillis, message);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_LIFECHAIN, delayRoutingKey, message, msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(delayMillis));
            return msg;
        });
    }
}
