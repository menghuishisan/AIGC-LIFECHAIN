package com.lifechain.chain.adapter;

import com.lifechain.chain.model.ChainSubmitResult;

import java.time.LocalDateTime;

/**
 * 结算链码适配器接口
 * <p>
 * 封装 {@code settlement_chaincode} 链码的全部调用方法。
 * 分账结算和逆分账信息上链后，确保资金流转的全链路可追溯。
 * </p>
 *
 * @author LifeChain
 */
public interface SettlementChainAdapter {

    /**
     * 结算记录上链
     *
     * @param bizId       业务ID
     * @param settleNo    结算编号
     * @param orderNo     关联订单编号
     * @param totalAmount 结算总金额（分）
     * @param summaryHash 结算摘要哈希
     * @param settleTime  结算时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerSettlement(Long bizId, String settleNo, String orderNo,
                                         Long totalAmount, String summaryHash,
                                         LocalDateTime settleTime);

    /**
     * 逆分账记录上链
     *
     * @param bizId         业务ID
     * @param reverseNo     逆分账编号
     * @param settleNo      原结算编号
     * @param reverseAmount 逆分账金额（分）
     * @param reason        逆分账原因
     * @param reverseTime   逆分账时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerReverseSettlement(Long bizId, String reverseNo, String settleNo,
                                                Long reverseAmount, String reason,
                                                LocalDateTime reverseTime);
}
