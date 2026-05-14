package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 交易订单实体
 * <p>
 * 对应数据库表 {@code trade_order}，记录AIGC作品授权交易的订单信息，
 * 包括买卖双方、订单状态、支付渠道与状态、过期时间及各关键时间戳。
 * 订单从创建到完成需经历状态机严格管控的完整生命周期。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order")
public class TradeOrderEntity extends BaseEntity {

    /** 订单编号（对外唯一标识） */
    @TableField("order_no")
    private String orderNo;

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 上架记录ID */
    @TableField("listing_id")
    private Long listingId;

    /** 上架编号 */
    @TableField("listing_no")
    private String listingNo;

    /** 买方账户ID */
    @TableField("buyer_account_id")
    private Long buyerAccountId;

    /** 买方主体ID */
    @TableField("buyer_subject_id")
    private Long buyerSubjectId;

    /** 创作者账户ID */
    @TableField("creator_account_id")
    private Long creatorAccountId;

    /** 创作者主体ID */
    @TableField("creator_subject_id")
    private Long creatorSubjectId;

    /** 订单状态 */
    @TableField("order_status")
    private String orderStatus;

    /** 授权类型 */
    @TableField("license_type")
    private String licenseType;

    /** 原价（单位：分） */
    @TableField("price_amount")
    private Long priceAmount;

    /** 实付金额（单位：分） */
    @TableField("pay_amount")
    private Long payAmount;

    /** 币种 */
    @TableField("currency")
    private String currency;

    /** 支付渠道 */
    @TableField("pay_channel")
    private String payChannel;

    /** 支付状态 */
    @TableField("pay_status")
    private String payStatus;

    /** 订单过期时间（UTC） */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 支付时间（UTC） */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /** 完成时间（UTC） */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /** 取消时间（UTC） */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /** 取消原因 */
    @TableField("cancel_reason")
    private String cancelReason;

    /** 幂等请求ID */
    @TableField("request_id")
    private String requestId;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
