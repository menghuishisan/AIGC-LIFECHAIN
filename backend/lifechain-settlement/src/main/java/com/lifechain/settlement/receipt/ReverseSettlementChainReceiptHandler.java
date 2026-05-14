package com.lifechain.settlement.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.SettlementStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.settlement.entity.ReverseSettlementRecordEntity;
import com.lifechain.settlement.entity.SettlementRecordEntity;
import com.lifechain.settlement.mapper.ReverseSettlementRecordMapper;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReverseSettlementChainReceiptHandler implements ChainReceiptHandler {

    private final ReverseSettlementRecordMapper reverseSettlementRecordMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final AuditService auditService;

    @Override
    public String getBizType() {
        return BizTypeEnum.REVERSE_SETTLEMENT.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        ReverseSettlementRecordEntity reverse = reverseSettlementRecordMapper.selectById(record.getBizId());
        if (reverse == null) {
            log.warn("逆分账回执处理失败，未找到逆分账记录，bizId={}", record.getBizId());
            return;
        }
        String fromStatus = reverse.getStatus();
        if (!SettlementStatusEnum.REVERSE_PENDING.getCode().equals(fromStatus)
                || !ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(reverse.getChainStatus())) {
            return;
        }
        reverse.setStatus(SettlementStatusEnum.REVERSE_SUCCESS.getCode());
        reverse.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        reverse.setCompleteTime(DateTimeUtil.nowUtc());
        reverse.setTxHash(record.getTxHash());
        reverse.setBlockHeight(record.getBlockHeight());
        reverseSettlementRecordMapper.updateById(reverse);

        auditService.writeStatusHistory(BizTypeEnum.REVERSE_SETTLEMENT.getCode(), reverse.getId(), reverse.getReverseNo(),
                fromStatus, SettlementStatusEnum.REVERSE_SUCCESS.getCode(),
                "链上回执确认成功", null, null);

        // 联动更新原结算记录状态为 REVERSE_SUCCESS
        SettlementRecordEntity settlement = settlementRecordMapper.selectById(reverse.getSettleId());
        if (settlement != null) {
            String settleFromStatus = settlement.getStatus();
            settlement.setStatus(SettlementStatusEnum.REVERSE_SUCCESS.getCode());
            settlement.setUpdatedAt(DateTimeUtil.nowUtc());
            settlementRecordMapper.updateById(settlement);
            auditService.writeStatusHistory(BizTypeEnum.SETTLEMENT.getCode(), settlement.getId(), settlement.getSettleNo(),
                    settleFromStatus, SettlementStatusEnum.REVERSE_SUCCESS.getCode(),
                    "逆分账链上确认成功，原结算已逆转", null, null);
        }

        log.info("逆分账回执处理完成，reverseNo={} -> REVERSE_SUCCESS", reverse.getReverseNo());
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        ReverseSettlementRecordEntity reverse = reverseSettlementRecordMapper.selectById(record.getBizId());
        if (reverse == null) return;
        String fromStatus = reverse.getStatus();
        reverse.setStatus(SettlementStatusEnum.REVERSE_FAILED.getCode());
        reverse.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        reverse.setFailReason(record.getFailReason());
        reverseSettlementRecordMapper.updateById(reverse);

        auditService.writeStatusHistory(BizTypeEnum.REVERSE_SETTLEMENT.getCode(), reverse.getId(), reverse.getReverseNo(),
                fromStatus, SettlementStatusEnum.REVERSE_FAILED.getCode(),
                "链上回执失败: " + record.getFailReason(), null, null);

        // 逆分账失败时必须恢复原结算状态为 SETTLE_SUCCESS
        SettlementRecordEntity settlement = settlementRecordMapper.selectById(reverse.getSettleId());
        if (settlement != null && SettlementStatusEnum.REVERSE_PENDING.getCode().equals(settlement.getStatus())) {
            String settleFromStatus = settlement.getStatus();
            settlement.setStatus(SettlementStatusEnum.SETTLE_SUCCESS.getCode());
            settlement.setUpdatedAt(DateTimeUtil.nowUtc());
            settlementRecordMapper.updateById(settlement);
            auditService.writeStatusHistory(BizTypeEnum.SETTLEMENT.getCode(), settlement.getId(), settlement.getSettleNo(),
                    settleFromStatus, SettlementStatusEnum.SETTLE_SUCCESS.getCode(),
                    "逆分账链上失败，恢复原结算状态", null, null);
            log.info("逆分账失败，原结算状态已恢复: settleNo={}", settlement.getSettleNo());
        }
    }
}
