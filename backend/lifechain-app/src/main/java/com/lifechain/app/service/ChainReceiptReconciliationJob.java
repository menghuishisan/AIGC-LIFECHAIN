package com.lifechain.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.chain.record.ChainTxRecordMapper;
import com.lifechain.chain.service.FabricChainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 持续确认交易验证码并重试业务回写，补足单次MQ消息丢失或查询结果未知。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainReceiptReconciliationJob {
    private final ChainTxRecordMapper mapper;
    private final FabricChainService chain;
    private long cursor;

    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void reconcile() {
        var records = mapper.selectPage(new Page<ChainTxRecordEntity>(1, 50, false),
                new LambdaQueryWrapper<ChainTxRecordEntity>().gt(ChainTxRecordEntity::getId, cursor)
                        .eq(ChainTxRecordEntity::getReceiptProcessed, false)
                        .in(ChainTxRecordEntity::getChainStatus, "CHAIN_SUBMITTED", "CHAIN_SUCCESS", "CHAIN_FAILED")
                        .orderByAsc(ChainTxRecordEntity::getId)).getRecords();
        for (var record : records) {
            cursor = record.getId();
            try { chain.retrySubmit(record.getId()); }
            catch (Exception e) { log.warn("链回执待下轮补偿: bizNo={}", record.getBizNo()); }
        }
        if (records.size() < 50) cursor = 0;
    }
}
