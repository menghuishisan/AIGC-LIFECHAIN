package com.lifechain.settlement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 结算明细实体
 * <p>
 * 对应数据库表 {@code settlement_item}，记录每笔结算中各角色的分账金额明细。
 * 每条结算记录对应多条明细，分别记录创作者和平台等各方的实际收益金额。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("settlement_item")
public class SettlementItemEntity extends BaseEntity {

    /** 结算记录ID */
    @TableField("settle_id")
    private Long settleId;

    /** 结算编号 */
    @TableField("settle_no")
    private String settleNo;

    /** 收款账户ID */
    @TableField("account_id")
    private Long accountId;

    /** 角色类型（CREATOR/PLATFORM/OTHER） */
    @TableField("role_type")
    private String roleType;

    /** 分账比例 */
    @TableField("ratio")
    private BigDecimal ratio;

    /** 金额（单位：分） */
    @TableField("amount")
    private Long amount;

    /** 状态（PENDING/SUCCESS/FAILED） */
    @TableField("status")
    private String status;
}
