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
 * 消费链上补偿消息，查询待确认交易，并重试最终成功或失败结果的业务回写。
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
     * 待确认时查询链结果，已有最终结果时只重试业务回写。
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
            // 链结果与业务回写分别确认，避免已确认失败的回执在业务事务失败后遗漏补偿。
            if (Boolean.TRUE.equals(record.getReceiptProcessed())
                    || !java.util.Set.of("CHAIN_SUBMITTED", "CHAIN_SUCCESS", "CHAIN_FAILED").contains(record.getChainStatus())) {
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
