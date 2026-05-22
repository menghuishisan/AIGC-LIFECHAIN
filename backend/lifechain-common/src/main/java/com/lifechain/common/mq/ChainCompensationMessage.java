package com.lifechain.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 链上交易补偿消息
 * <p>
 * 链上交易提交后发送到延迟队列，TTL 到期后由 ChainCompensationConsumer 消费，
 * 检查交易是否已收到回执，未确认则执行重试提交。
 * </p>
 *
 * @author LifeChain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChainCompensationMessage implements Serializable {
    /** 链上交易记录ID */
    private Long recordId;
    /** 业务类型 */
    private String bizType;
    /** 业务编号 */
    private String bizNo;
}
