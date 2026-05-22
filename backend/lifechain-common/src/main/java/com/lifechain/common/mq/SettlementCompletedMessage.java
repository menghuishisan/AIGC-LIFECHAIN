package com.lifechain.common.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 结算完成消息
 * <p>
 * 结算链上回执确认成功后发送，通知订单模块将订单状态推进为 ORDER_COMPLETED。
 * </p>
 *
 * @author LifeChain
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCompletedMessage implements Serializable {
    /** 订单编号 */
    private String orderNo;
    /** 订单ID */
    private Long orderId;
    /** 结算单号 */
    private String settleNo;
}
