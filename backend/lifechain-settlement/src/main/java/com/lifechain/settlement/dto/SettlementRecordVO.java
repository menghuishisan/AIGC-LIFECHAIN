package com.lifechain.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算记录视图对象
 * <p>
 * 展示结算记录完整信息，包含结算状态、链上状态、区块信息以及各角色分账明细。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class SettlementRecordVO implements Serializable {

    /** 结算编号 */
    private String settleNo;

    /** 订单编号 */
    private String orderNo;

    /** 作品编号 */
    private String workNo;

    /** 结算总金额（单位：分） */
    private Long totalAmount;

    /** 结算状态 */
    private String settleStatus;

    /** 链上状态 */
    private String chainStatus;

    /** 交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockHeight;

    /** 结算时间 */
    private LocalDateTime settleTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 结算明细列表 */
    private List<SettlementItemVO> items;
}
