package com.lifechain.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单过期消息
 * <p>
 * 订单创建时发送到延迟队列，TTL 到期后由 OrderExpireConsumer 消费，
 * 自动将超时未支付的订单标记为 ORDER_EXPIRED。
 * </p>
 *
 * @author LifeChain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderExpireMessage implements Serializable {
    /** 订单ID */
    private Long orderId;
    /** 订单编号 */
    private String orderNo;
}
