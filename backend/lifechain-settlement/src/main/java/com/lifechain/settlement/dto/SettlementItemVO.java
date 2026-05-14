package com.lifechain.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 结算明细视图对象
 * <p>
 * 展示单个角色在一笔结算中的分账比例和实际收益金额。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class SettlementItemVO implements Serializable {

    /** 角色类型 */
    private String roleType;

    /** 收款账户编号 */
    private String accountNo;

    /** 分账比例 */
    private BigDecimal ratio;

    /** 结算金额（单位：分） */
    private Long amount;

    /** 明细状态 */
    private String itemStatus;
}
