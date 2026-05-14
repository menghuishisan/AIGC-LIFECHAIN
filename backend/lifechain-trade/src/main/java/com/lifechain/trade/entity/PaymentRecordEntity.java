package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 支付记录实体
 * <p>
 * 对应数据库表 {@code payment_record}，记录每笔交易订单的支付明细，
 * 包括支付渠道、支付状态、第三方交易号、回调原始报文索引等。
 * 一笔订单可对应多条支付记录（如取消后重新支付），但同一时刻只有一条有效记录。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecordEntity extends BaseEntity {

    /** 支付编号（对外唯一标识） */
    @TableField("payment_no")
    private String paymentNo;

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 支付渠道（WECHAT_PAY/ALIPAY） */
    @TableField("pay_channel")
    private String payChannel;

    /** 支付状态 */
    @TableField("pay_status")
    private String payStatus;

    /** 支付金额（单位：分） */
    @TableField("pay_amount")
    private Long payAmount;

    /** 币种 */
    @TableField("currency")
    private String currency;

    /** 第三方交易号 */
    @TableField("third_trade_no")
    private String thirdTradeNo;

    /** 预支付ID */
    @TableField("prepay_id")
    private String prepayId;

    /** 回调原始报文索引 */
    @TableField("callback_raw_ref")
    private String callbackRawRef;

    /** 支付时间（UTC） */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /** 回调时间（UTC） */
    @TableField("callback_time")
    private LocalDateTime callbackTime;

    /** 过期时间（UTC） */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 幂等请求ID */
    @TableField("request_id")
    private String requestId;
}
