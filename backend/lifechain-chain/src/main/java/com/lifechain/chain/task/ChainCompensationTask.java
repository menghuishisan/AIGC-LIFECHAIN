package com.lifechain.chain.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.chain.record.ChainTxRecordMapper;
import com.lifechain.chain.service.FabricChainService;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 链上交易补偿定时任务
 * <p>
 * 以固定延迟（60秒）周期性执行，承担两项职责：
 * <ol>
 *   <li><b>超时重试：</b>扫描所有处于 {@code CHAIN_SUBMITTED}（已提交待回执）状态超过5分钟的记录，
 *       通过 QSCC 系统链码验证交易是否已上链，更新记录的最终状态。</li>
 *   <li><b>异常检测：</b>扫描所有处于 {@code CHAIN_FAILED}（链上失败）的记录并记录告警日志，
 *       便于运维排查和人工介入。</li>
 * </ol>
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainCompensationTask {

    /** 超时阈值：提交后5分钟未收到回执视为超时 */
    private static final long TIMEOUT_MINUTES = 5;

    /** 单次补偿扫描最大处理记录数，避免单次任务执行时间过长 */
    private static final int MAX_BATCH_SIZE = 100;

    private final ChainTxRecordMapper chainTxRecordMapper;
    private final FabricChainService fabricChainService;

    /**
     * 定时执行链上交易补偿
     * <p>
     * 固定延迟60秒执行一次。每次执行依次完成：
     * <ol>
     *   <li>处理超时的 CHAIN_SUBMITTED 记录</li>
     *   <li>检测 CHAIN_FAILED 异常记录</li>
     * </ol>
     * 任务执行中的异常被捕获并记录日志，不会影响下一次调度。
     * </p>
     */
    @Scheduled(fixedDelay = 60000)
    public void execute() {
        try {
            compensateSubmittedRecords();
            detectFailedRecords();
        } catch (Exception e) {
            log.error("链上交易补偿任务执行异常", e);
        }
    }

    /**
     * 补偿超时的 CHAIN_SUBMITTED 记录
     * <p>
     * 查找提交时间超过5分钟的 CHAIN_SUBMITTED 记录，逐条调用
     * {@link FabricChainService#retrySubmit(Long)} 验证交易是否已上链。
     * </p>
     */
    private void compensateSubmittedRecords() {
        LocalDateTime timeoutBefore = DateTimeUtil.nowUtc().minusMinutes(TIMEOUT_MINUTES);

        LambdaQueryWrapper<ChainTxRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTxRecordEntity::getChainStatus, ChainStatusEnum.CHAIN_SUBMITTED.getCode())
                .le(ChainTxRecordEntity::getSubmitTime, timeoutBefore)
                .orderByAsc(ChainTxRecordEntity::getSubmitTime)
                .last("LIMIT " + MAX_BATCH_SIZE);

        List<ChainTxRecordEntity> records = chainTxRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return;
        }

        log.info("链上补偿任务开始，待处理超时 CHAIN_SUBMITTED 记录数={}", records.size());

        int successCount = 0;
        int failCount = 0;

        for (ChainTxRecordEntity record : records) {
            try {
                log.info("补偿处理链上交易，recordId={}, bizType={}, bizNo={}, txHash={}",
                        record.getId(), record.getBizType(), record.getBizNo(), record.getTxHash());

                var result = fabricChainService.retrySubmit(record.getId());
                if (result.isSuccess()) {
                    successCount++;
                    log.info("链上交易补偿成功，recordId={}, txHash={}", record.getId(), record.getTxHash());
                } else {
                    failCount++;
                    log.warn("链上交易补偿后仍为失败，recordId={}, txHash={}, 原因={}",
                            record.getId(), record.getTxHash(), result.getFailReason());
                }
            } catch (Exception e) {
                failCount++;
                log.error("链上交易补偿异常，recordId={}, bizType={}, bizNo={}",
                        record.getId(), record.getBizType(), record.getBizNo(), e);
            }
        }

        log.info("链上补偿任务完成，总数={}，成功={}，失败={}", records.size(), successCount, failCount);
    }

    /**
     * 检测 CHAIN_FAILED 异常记录
     * <p>
     * 扫描处于 CHAIN_FAILED 状态的记录，记录告警日志。
     * 这些记录可能需要业务层重新发起上链操作或运维人工介入处理。
     * </p>
     */
    private void detectFailedRecords() {
        LambdaQueryWrapper<ChainTxRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChainTxRecordEntity::getChainStatus, ChainStatusEnum.CHAIN_FAILED.getCode())
                .orderByAsc(ChainTxRecordEntity::getCreatedAt)
                .last("LIMIT " + MAX_BATCH_SIZE);

        List<ChainTxRecordEntity> records = chainTxRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return;
        }

        log.warn("检测到 CHAIN_FAILED 链上失败记录，数量={}，需关注处理", records.size());

        for (ChainTxRecordEntity record : records) {
            log.warn("链上失败记录详情：recordId={}, bizType={}, bizNo={}, txHash={}, failReason={}, reasonCode={}",
                    record.getId(), record.getBizType(), record.getBizNo(),
                    record.getTxHash(), record.getFailReason(), record.getReasonCode());
        }
    }
}
