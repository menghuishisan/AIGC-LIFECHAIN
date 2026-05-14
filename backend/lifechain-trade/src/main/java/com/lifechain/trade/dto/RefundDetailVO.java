package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退款记录详情视图对象
 *
 * @author LifeChain
 */
@Data
public class RefundDetailVO implements Serializable {

    /** 退款编号 */
    private String refundNo;

    /** 订单编号 */
    private String orderNo;

    /** 支付编号 */
    private String paymentNo;

    /** 支付渠道 */
    private String payChannel;

    /** 退款金额（单位：分） */
    private Long refundAmount;

    /** 币种 */
    private String currency;

    /** 退款状态 */
    private String refundStatus;

    /** 退款原因 */
    private String refundReason;

    /** 第三方退款流水号 */
    private String thirdRefundNo;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 失败原因 */
    private String failReason;
}
