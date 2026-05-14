package com.lifechain.settlement.task;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.adapter.SettlementChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.SettlementStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.settlement.entity.SettlementItemEntity;
import com.lifechain.settlement.entity.SettlementRecordEntity;
import com.lifechain.settlement.mapper.SettlementItemMapper;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 结算定时任务
 * <p>
 * 包含两个定时任务：
 * <ol>
 *   <li>结算失败自动重试：每120秒扫描 SETTLE_FAILED 的记录，重新提交链上存证</li>
 *   <li>每日对账检查：每天凌晨2点扫描状态不一致的记录，输出告警日志</li>
 * </ol>
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementTask {

    private final SettlementRecordMapper settlementRecordMapper;
    private final SettlementItemMapper settlementItemMapper;
    private final SettlementChainAdapter settlementChainAdapter;
    private final AuditService auditService;

    /** 单次最大重试记录数，防止堆积导致长时间阻塞 */
    private static final int MAX_RETRY_BATCH = 50;
    /** 最大重试次数，超过后不再自动重试 */
    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 结算失败自动重试任务
     * <p>
     * 每120秒（2分钟）扫描一次状态为 SETTLE_FAILED 且重试次数未超限的结算记录，
     * 逐笔重新提交链上存证。每批次最多处理 {@value #MAX_RETRY_BATCH} 条记录。
     * </p>
     */
    @Scheduled(fixedDelay = 120000)
    public void retryFailedSettlements() {
        List<SettlementRecordEntity> failedRecords = settlementRecordMapper.selectFailedRecords();
        if (failedRecords.isEmpty()) {
            return;
        }

        log.info("扫描到{}笔失败结算记录，开始自动重试", failedRecords.size());
        int processedCount = 0;

        for (SettlementRecordEntity record : failedRecords) {
            if (processedCount >= MAX_RETRY_BATCH) {
                log.info("本批次已达最大重试数量{}，剩余记录待下次处理", MAX_RETRY_BATCH);
                break;
            }
            if (record.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("结算重试次数已达上限，跳过: settleNo={}, retryCount={}",
                        record.getSettleNo(), record.getRetryCount());
                continue;
            }
            try {
                processRetry(record);
                processedCount++;
            } catch (Exception e) {
                log.error("结算自动重试异常: settleNo={}", record.getSettleNo(), e);
            }
        }

        log.info("结算自动重试完成，本批次处理{}笔", processedCount);
    }

    /**
     * 每日对账检查任务
     * <p>
     * 每天凌晨2点执行，扫描以下异常状态的结算记录并输出告警日志：
     * <ul>
     *   <li>结算成功但链上状态非成功</li>
     *   <li>长时间停留在 SETTLE_PROCESSING 状态</li>
     * </ul>
     * </p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReconciliation() {
        log.info("开始每日结算对账检查");

        List<SettlementRecordEntity> mismatchedRecords = settlementRecordMapper.selectMismatchedRecords();
        if (mismatchedRecords.isEmpty()) {
            log.info("对账检查完成，无异常记录");
            return;
        }

        log.warn("对账检查发现{}笔异常记录", mismatchedRecords.size());
        for (SettlementRecordEntity record : mismatchedRecords) {
            log.warn("对账异常: settleNo={}, status={}, chainStatus={}, settleTime={}, totalAmount={}",
                    record.getSettleNo(), record.getStatus(), record.getChainStatus(),
                    record.getSettleTime(), record.getTotalAmount());
        }

        log.info("每日结算对账检查完成，发现{}笔异常", mismatchedRecords.size());
    }

    /**
     * 处理单笔结算重试
     * <p>
     * 重新构建摘要哈希并提交链上存证，根据回执结果更新状态。
     * 重试次数累加，成功后更新明细状态。
     * </p>
     *
     * @param record 失败的结算记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void processRetry(SettlementRecordEntity record) {
        String settleNo = record.getSettleNo();
        log.info("自动重试结算: settleNo={}, retryCount={}", settleNo, record.getRetryCount());

        List<SettlementItemEntity> items = settlementItemMapper.selectBySettleId(record.getId());
        String summaryHash = buildSummaryHash(settleNo, items);

        String fromStatus = record.getStatus();
        record.setRetryCount(record.getRetryCount() + 1);
        record.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        record.setFailReason(null);

        try {
            ChainSubmitResult chainResult = settlementChainAdapter.registerSettlement(
                    record.getId(), settleNo, record.getOrderNo(),
                    record.getTotalAmount(), summaryHash, record.getSettleTime());

            if (chainResult.isSuccess()) {
                record.setStatus(SettlementStatusEnum.SETTLE_PROCESSING.getCode());
                record.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                record.setTxHash(chainResult.getTxHash());
                log.info("自动重试链提交成功，等待回执: settleNo={}, txHash={}", settleNo, chainResult.getTxHash());
            } else {
                record.setStatus(SettlementStatusEnum.SETTLE_FAILED.getCode());
                record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                record.setFailReason(chainResult.getFailReason());
                log.warn("自动重试失败: settleNo={}, reason={}", settleNo, chainResult.getFailReason());
            }
        } catch (Exception e) {
            record.setStatus(SettlementStatusEnum.SETTLE_FAILED.getCode());
            record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            record.setFailReason(e.getMessage());
            log.error("自动重试异常: settleNo={}", settleNo, e);
        }

        record.setUpdatedAt(DateTimeUtil.nowUtc());
        settlementRecordMapper.updateById(record);

        auditService.writeStatusHistory(
                BizTypeEnum.SETTLEMENT.getCode(), record.getId(), settleNo,
                fromStatus, record.getStatus(),
                "定时任务自动重试(第" + record.getRetryCount() + "次)", null, null);
    }

    /**
     * 构建结算摘要哈希
     *
     * @param settleNo 结算编号
     * @param items    结算明细列表
     * @return SHA-256哈希值
     */
    private String buildSummaryHash(String settleNo, List<SettlementItemEntity> items) {
        StringBuilder sb = new StringBuilder(settleNo);
        for (SettlementItemEntity item : items) {
            sb.append("|").append(item.getRoleType())
                    .append(":").append(item.getAccountId())
                    .append(":").append(item.getRatio())
                    .append(":").append(item.getAmount());
        }
        return HashUtil.sha256(sb.toString());
    }
}
