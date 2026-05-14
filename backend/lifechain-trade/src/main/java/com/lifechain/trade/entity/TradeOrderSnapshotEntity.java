package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 交易订单快照实体
 * <p>
 * 对应数据库表 {@code trade_order_snapshot}，保存订单创建时的作品信息、
 * 上架信息和分账规则等快照，确保交易全程数据可追溯、不可篡改。
 * 快照类型包括：WORK_INFO（作品信息）、LISTING_INFO（上架信息）、SETTLE_RULE（分账规则）。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trade_order_snapshot")
public class TradeOrderSnapshotEntity extends BaseEntity {

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 快照类型（WORK_INFO/LISTING_INFO/SETTLE_RULE） */
    @TableField("snapshot_type")
    private String snapshotType;

    /** 快照数据（JSON格式） */
    @TableField("snapshot_data")
    private String snapshotData;

    /** 快照哈希（SHA-256） */
    @TableField("snapshot_hash")
    private String snapshotHash;

    /** 快照时间（UTC） */
    @TableField("snapshot_time")
    private LocalDateTime snapshotTime;
}
