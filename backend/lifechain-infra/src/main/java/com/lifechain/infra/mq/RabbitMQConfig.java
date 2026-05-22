package com.lifechain.infra.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 统一配置
 * <p>
 * 声明平台所有交换机、队列和绑定关系。
 * 延迟消息通过 TTL + 死信队列实现，不依赖 delayed_message_exchange 插件。
 * </p>
 *
 * @author LifeChain
 */
@Configuration
public class RabbitMQConfig {

    /** 业务直连交换机 */
    public static final String EXCHANGE_LIFECHAIN = "lifechain.exchange";
    /** 延迟死信交换机（接收 TTL 过期消息） */
    public static final String EXCHANGE_DEAD_LETTER = "lifechain.dlx.exchange";

    /** 结算完成队列 */
    public static final String QUEUE_SETTLEMENT_COMPLETED = "lifechain.settlement.completed";
    /** 特征提取队列 */
    public static final String QUEUE_FEATURE_EXTRACT = "lifechain.feature.extract";
    /** 订单过期队列（实际消费队列） */
    public static final String QUEUE_ORDER_EXPIRE = "lifechain.order.expire";
    /** 订单过期延迟队列（TTL 到期后转发到消费队列） */
    public static final String QUEUE_ORDER_EXPIRE_DELAY = "lifechain.order.expire.delay";
    /** 链上补偿队列（实际消费队列） */
    public static final String QUEUE_CHAIN_COMPENSATION = "lifechain.chain.compensation";
    /** 链上补偿延迟队列 */
    public static final String QUEUE_CHAIN_COMPENSATION_DELAY = "lifechain.chain.compensation.delay";

    /** 结算完成路由键 */
    public static final String RK_SETTLEMENT_COMPLETED = "settlement.completed";
    /** 特征提取路由键 */
    public static final String RK_FEATURE_EXTRACT = "feature.extract";
    /** 订单过期路由键 */
    public static final String RK_ORDER_EXPIRE = "order.expire";
    /** 订单过期延迟路由键 */
    public static final String RK_ORDER_EXPIRE_DELAY = "order.expire.delay";
    /** 链上补偿路由键 */
    public static final String RK_CHAIN_COMPENSATION = "chain.compensation";
    /** 链上补偿延迟路由键 */
    public static final String RK_CHAIN_COMPENSATION_DELAY = "chain.compensation.delay";

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }

    // ==================== 交换机 ====================

    @Bean
    public DirectExchange lifechainExchange() {
        return new DirectExchange(EXCHANGE_LIFECHAIN, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DEAD_LETTER, true, false);
    }

    // ==================== 业务队列 ====================

    @Bean
    public Queue settlementCompletedQueue() {
        return QueueBuilder.durable(QUEUE_SETTLEMENT_COMPLETED).build();
    }

    @Bean
    public Queue featureExtractQueue() {
        return QueueBuilder.durable(QUEUE_FEATURE_EXTRACT).build();
    }

    @Bean
    public Queue orderExpireQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_EXPIRE).build();
    }

    @Bean
    public Queue chainCompensationQueue() {
        return QueueBuilder.durable(QUEUE_CHAIN_COMPENSATION).build();
    }

    // ==================== 延迟队列（TTL + DLX） ====================

    @Bean
    public Queue orderExpireDelayQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_EXPIRE_DELAY)
                .deadLetterExchange(EXCHANGE_DEAD_LETTER)
                .deadLetterRoutingKey(RK_ORDER_EXPIRE)
                .build();
    }

    @Bean
    public Queue chainCompensationDelayQueue() {
        return QueueBuilder.durable(QUEUE_CHAIN_COMPENSATION_DELAY)
                .deadLetterExchange(EXCHANGE_DEAD_LETTER)
                .deadLetterRoutingKey(RK_CHAIN_COMPENSATION)
                .build();
    }

    // ==================== 绑定关系 ====================

    @Bean
    public Binding settlementCompletedBinding() {
        return BindingBuilder.bind(settlementCompletedQueue()).to(lifechainExchange()).with(RK_SETTLEMENT_COMPLETED);
    }

    @Bean
    public Binding featureExtractBinding() {
        return BindingBuilder.bind(featureExtractQueue()).to(lifechainExchange()).with(RK_FEATURE_EXTRACT);
    }

    @Bean
    public Binding orderExpireDelayBinding() {
        return BindingBuilder.bind(orderExpireDelayQueue()).to(lifechainExchange()).with(RK_ORDER_EXPIRE_DELAY);
    }

    @Bean
    public Binding chainCompensationDelayBinding() {
        return BindingBuilder.bind(chainCompensationDelayQueue()).to(lifechainExchange()).with(RK_CHAIN_COMPENSATION_DELAY);
    }

    @Bean
    public Binding orderExpireBinding() {
        return BindingBuilder.bind(orderExpireQueue()).to(deadLetterExchange()).with(RK_ORDER_EXPIRE);
    }

    @Bean
    public Binding chainCompensationBinding() {
        return BindingBuilder.bind(chainCompensationQueue()).to(deadLetterExchange()).with(RK_CHAIN_COMPENSATION);
    }
}
