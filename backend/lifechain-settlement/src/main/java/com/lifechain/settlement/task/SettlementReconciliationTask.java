package com.lifechain.settlement.task;

import com.lifechain.settlement.entity.SettlementRecordEntity;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 结算每日对账定时任务
 * <p>
 * 每天凌晨2点执行，扫描结算成功但链上状态不一致、
 * 或长时间停留在 SETTLE_PROCESSING 状态的异常记录并输出告警日志。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementReconciliationTask {

    private final SettlementRecordMapper settlementRecordMapper;

    /**
     * 每日结算对账
     * <p>
     * 每天凌晨2点执行，扫描结算成功但链上状态不一致或长时间停留在
     * SETTLE_PROCESSING 状态的异常记录，输出告警日志供运维排查。
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
    }
}
