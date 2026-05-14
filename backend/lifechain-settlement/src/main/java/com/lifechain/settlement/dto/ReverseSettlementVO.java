package com.lifechain.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 逆分账记录视图对象
 * <p>
 * 展示逆分账操作的完整信息，包括逆分账金额、状态、链上存证及操作时间。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ReverseSettlementVO implements Serializable {

    /** 逆分账编号 */
    private String reverseNo;

    /** 原结算编号 */
    private String settleNo;

    /** 订单编号 */
    private String orderNo;

    /** 逆分账金额（单位：分） */
    private Long reverseAmount;

    /** 逆分账状态 */
    private String reverseStatus;

    /** 链上状态 */
    private String chainStatus;

    /** 交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockHeight;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 完成时间 */
    private LocalDateTime completeTime;
}
