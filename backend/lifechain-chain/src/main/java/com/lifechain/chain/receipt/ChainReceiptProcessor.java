package com.lifechain.chain.receipt;

import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.ChainStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一链回执分发处理器
 * <p>
 * 收集所有 {@link ChainReceiptHandler} 实现，根据 bizType 分发到对应处理器，
 * 完成链上交易成功/失败后的业务对象状态回写。
 * </p>
 */
@Slf4j
@Component
public class ChainReceiptProcessor {

    private final Map<String, ChainReceiptHandler> handlerMap = new HashMap<>();

    public ChainReceiptProcessor(List<ChainReceiptHandler> handlers) {
        for (ChainReceiptHandler handler : handlers) {
            handlerMap.put(handler.getBizType(), handler);
        }
        log.info("链回执处理器注册完成，已注册 {} 个处理器: {}", handlerMap.size(), handlerMap.keySet());
    }

    /**
     * 处理链上交易回执
     * <p>
     * 根据 chainStatus 决定调用 onChainSuccess 还是 onChainFailed。
     * </p>
     *
     * @param record 已更新最终链状态的交易记录
     */
    public void processReceipt(ChainTxRecordEntity record) {
        String bizType = record.getBizType();
        ChainReceiptHandler handler = handlerMap.get(bizType);
        if (handler == null) {
            log.warn("未找到 bizType={} 的链回执处理器，跳过业务状态回写", bizType);
            return;
        }

        String chainStatus = record.getChainStatus();
        try {
            if (ChainStatusEnum.CHAIN_SUCCESS.getCode().equals(chainStatus)) {
                log.info("链回执成功，开始业务状态回写，bizType={}, bizNo={}, txHash={}",
                        bizType, record.getBizNo(), record.getTxHash());
                handler.onChainSuccess(record);
                log.info("业务状态回写完成，bizType={}, bizNo={}", bizType, record.getBizNo());
            } else if (ChainStatusEnum.CHAIN_FAILED.getCode().equals(chainStatus)) {
                log.info("链回执失败，开始业务失败处理，bizType={}, bizNo={}, failReason={}",
                        bizType, record.getBizNo(), record.getFailReason());
                handler.onChainFailed(record);
                log.info("业务失败处理完成，bizType={}, bizNo={}", bizType, record.getBizNo());
            }
        } catch (Exception e) {
            log.error("链回执业务状态回写异常，bizType={}, bizNo={}, txHash={}",
                    bizType, record.getBizNo(), record.getTxHash(), e);
        }
    }
}
