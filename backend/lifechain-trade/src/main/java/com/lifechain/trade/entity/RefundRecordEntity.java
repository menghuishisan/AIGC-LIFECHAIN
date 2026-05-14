package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 退款记录实体
 * <p>
 * 对应数据库表 {@code refund_record}，记录交易退款的完整信息，
 * 包括退款编号、关联支付记录、退款金额与状态、审批人及渠道退款流水号等。
 * 退款需管理员审批后触发渠道退款，退款成功后更新订单状态并触发逆分账。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund_record")
public class RefundRecordEntity extends BaseEntity {

    /** 退款编号（对外唯一标识） */
    @TableField("refund_no")
    private String refundNo;

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 支付记录ID */
    @TableField("payment_id")
    private Long paymentId;

    /** 支付编号 */
    @TableField("payment_no")
    private String paymentNo;

    /** 原支付渠道 */
    @TableField("pay_channel")
    private String payChannel;

    /** 退款金额（单位：分） */
    @TableField("refund_amount")
    private Long refundAmount;

    /** 币种 */
    @TableField("currency")
    private String currency;

    /** 退款状态（PENDING/SUCCESS/FAILED） */
    @TableField("refund_status")
    private String refundStatus;

    /** 退款原因 */
    @TableField("refund_reason")
    private String refundReason;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 渠道退款流水号 */
    @TableField("third_refund_no")
    private String thirdRefundNo;

    /** 申请时间（UTC） */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /** 完成时间（UTC） */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 幂等请求ID */
    @TableField("request_id")
    private String requestId;

    /** 退款前的订单状态（退款被拒绝时用于恢复） */
    @TableField("pre_refund_order_status")
    private String preRefundOrderStatus;
}
