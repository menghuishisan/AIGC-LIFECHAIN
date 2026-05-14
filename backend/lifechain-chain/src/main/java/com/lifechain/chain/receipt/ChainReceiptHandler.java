package com.lifechain.chain.receipt;

import com.lifechain.chain.record.ChainTxRecordEntity;

/**
 * 链上交易回执处理器接口
 * <p>
 * 各业务模块实现此接口，完成链上交易成功后的业务状态回写。
 * 统一回执处理器按 bizType 分发到对应实现。
 * </p>
 */
public interface ChainReceiptHandler {

    /**
     * 获取本处理器负责的业务类型编码
     */
    String getBizType();

    /**
     * 链上交易成功后的业务状态回写
     *
     * @param record 链上交易记录（已确认 CHAIN_SUCCESS）
     */
    void onChainSuccess(ChainTxRecordEntity record);

    /**
     * 链上交易最终失败后的业务状态回写
     *
     * @param record 链上交易记录（已确认 CHAIN_FAILED）
     */
    void onChainFailed(ChainTxRecordEntity record);
}
