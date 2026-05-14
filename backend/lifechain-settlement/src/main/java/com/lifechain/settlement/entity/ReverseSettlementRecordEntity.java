package com.lifechain.settlement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 逆分账记录实体
 * <p>
 * 对应数据库表 {@code reverse_settlement_record}，记录结算成功后发起的逆分账操作。
 * 逆分账通常由退款或争议仲裁触发，需将已分账资金按原路退回各方账户。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reverse_settlement_record")
public class ReverseSettlementRecordEntity extends BaseEntity {

    /** 逆分账编号（对外唯一标识） */
    @TableField("reverse_no")
    private String reverseNo;

    /** 原结算ID */
    @TableField("settle_id")
    private Long settleId;

    /** 原结算编号 */
    @TableField("settle_no")
    private String settleNo;

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 逆分账金额（单位：分） */
    @TableField("reverse_amount")
    private Long reverseAmount;

    /** 状态 */
    @TableField("status")
    private String status;

    /** 链上状态 */
    @TableField("chain_status")
    private String chainStatus;

    /** 原因 */
    @TableField("reason")
    private String reason;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 交易哈希 */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;

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

    /** 幂等ID */
    @TableField("request_id")
    private String requestId;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
