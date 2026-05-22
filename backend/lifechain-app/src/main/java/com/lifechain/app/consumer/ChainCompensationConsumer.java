package com.lifechain.app.consumer;

import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.chain.record.ChainTxRecordMapper;
import com.lifechain.chain.service.FabricChainService;
import com.lifechain.common.mq.ChainCompensationMessage;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 链上交易补偿消息消费者
 * <p>
 * 消费延迟队列中到期的链上补偿消息，对 CHAIN_SUBMITTED 超时未确认的交易记录
 * 执行重试提交。若交易已被确认则跳过处理。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainCompensationConsumer {

    private final ChainTxRecordMapper chainTxRecordMapper;
    private final FabricChainService fabricChainService;

    /**
     * 处理链上交易补偿消息
     * <p>
     * 从延迟队列接收到期消息后，查询链上交易记录状态，
     * 若仍为 CHAIN_SUBMITTED（超时未确认）则执行重试提交。
     * </p>
     *
     * @param message     补偿消息体，包含 recordId 和 bizNo
     * @param channel     RabbitMQ 通道，用于手动 ACK/NACK
     * @param deliveryTag 消息投递标签
     * @throws IOException 通道 ACK/NACK 操作异常
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_CHAIN_COMPENSATION)
    public void handle(ChainCompensationMessage message, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到链上补偿消息: recordId={}, bizNo={}", message.getRecordId(), message.getBizNo());
        try {
            // 查询链上交易记录
            ChainTxRecordEntity record = chainTxRecordMapper.selectById(message.getRecordId());
            if (record == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            // 状态检查：仅对 CHAIN_SUBMITTED 状态执行补偿，已确认则跳过
            if (!com.lifechain.common.enums.ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(record.getChainStatus())) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 执行重试提交
            var result = fabricChainService.retrySubmit(record.getId());
            if (result.isSuccess()) {
                log.info("链上补偿成功: recordId={}", message.getRecordId());
            } else {
                log.warn("链上补偿失败: recordId={}, reason={}", message.getRecordId(), result.getFailReason());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理链上补偿消息异常: recordId={}", message.getRecordId(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
