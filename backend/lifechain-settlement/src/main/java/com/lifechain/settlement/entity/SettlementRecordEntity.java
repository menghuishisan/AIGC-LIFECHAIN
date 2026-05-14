package com.lifechain.settlement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 结算记录实体
 * <p>
 * 对应数据库表 {@code settlement_record}，记录每笔订单的分账结算信息，
 * 包含结算金额、结算状态、链上存证状态、区块信息等核心字段。
 * 结算流程：创建 → 上链 → 成功/失败 → 可逆分账。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("settlement_record")
public class SettlementRecordEntity extends BaseEntity {

    /** 结算编号（对外唯一标识） */
    @TableField("settle_no")
    private String settleNo;

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 结算总金额（单位：分） */
    @TableField("total_amount")
    private Long totalAmount;

    /** 结算状态 */
    @TableField("status")
    private String status;

    /** 链上状态 */
    @TableField("chain_status")
    private String chainStatus;

    /** 结算时间（UTC） */
    @TableField("settle_time")
    private LocalDateTime settleTime;

    /** 完成时间（UTC） */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /** 交易哈希 */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 重试次数 */
    @TableField("retry_count")
    private Integer retryCount;

    /** 幂等ID */
    @TableField("request_id")
    private String requestId;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
