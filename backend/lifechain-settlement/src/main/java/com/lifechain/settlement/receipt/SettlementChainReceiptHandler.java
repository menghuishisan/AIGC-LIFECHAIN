package com.lifechain.settlement.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.SettlementStatusEnum;
import com.lifechain.common.mq.SettlementCompletedMessage;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.infra.mq.MessagePublisher;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.lifechain.settlement.entity.SettlementRecordEntity;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 结算链上回执处理器
 * <p>
 * 处理结算交易的链上回执，成功时更新结算状态为 SETTLE_SUCCESS 并发送 MQ 消息通知订单完成，
 * 失败时更新为 SETTLE_FAILED 并记录失败原因。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementChainReceiptHandler implements ChainReceiptHandler {

    private final SettlementRecordMapper settlementRecordMapper;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final MessagePublisher messagePublisher;

    @Override
    public String getBizType() {
        return BizTypeEnum.SETTLEMENT.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        SettlementRecordEntity settlement = settlementRecordMapper.selectById(record.getBizId());
        if (settlement == null) {
            log.warn("结算回执处理失败，未找到结算记录，bizId={}", record.getBizId());
            return;
        }
        String fromStatus = settlement.getStatus();
        if (!SettlementStatusEnum.SETTLE_PROCESSING.getCode().equals(fromStatus)
                || !ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(settlement.getChainStatus())) {
            log.info("结算当前状态不需要回执处理，settleNo={}, status={}", settlement.getSettleNo(), fromStatus);
            return;
        }
        settlement.setStatus(SettlementStatusEnum.SETTLE_SUCCESS.getCode());
        settlement.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        settlement.setCompleteTime(DateTimeUtil.nowUtc());
        settlement.setTxHash(record.getTxHash());
        settlement.setBlockHeight(record.getBlockHeight());
        settlementRecordMapper.updateById(settlement);

        auditService.writeStatusHistory(BizTypeEnum.SETTLEMENT.getCode(), settlement.getId(), settlement.getSettleNo(),
                fromStatus, SettlementStatusEnum.SETTLE_SUCCESS.getCode(),
                "链上回执确认成功", null, null);
        auditService.writeAuditLog(BizTypeEnum.SETTLEMENT.getCode(), settlement.getId(), settlement.getSettleNo(),
                "CHAIN_RECEIPT_SUCCESS", "结算链上回执确认，txHash=" + record.getTxHash(),
                null, "SYSTEM", null, "SUCCESS", null);

        // 链上回执确认后写入真正的"结算完成"轨迹
        traceEventService.writeTraceEvent(BizTypeEnum.SETTLEMENT.getCode(), settlement.getId(), settlement.getSettleNo(),
                "SETTLEMENT_COMPLETED", "结算链上回执确认，结算完成", null, "SYSTEM", null);

        log.info("结算回执处理完成，settleNo={} -> SETTLE_SUCCESS", settlement.getSettleNo());

        // 发送结算成功消息，通知订单模块更新订单状态
        messagePublisher.send(RabbitMQConfig.RK_SETTLEMENT_COMPLETED,
                new SettlementCompletedMessage(settlement.getOrderNo(), settlement.getOrderId(), settlement.getSettleNo()));
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        SettlementRecordEntity settlement = settlementRecordMapper.selectById(record.getBizId());
        if (settlement == null) return;
        String fromStatus = settlement.getStatus();
        settlement.setStatus(SettlementStatusEnum.SETTLE_FAILED.getCode());
        settlement.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        settlement.setFailReason(record.getFailReason());
        settlementRecordMapper.updateById(settlement);

        auditService.writeStatusHistory(BizTypeEnum.SETTLEMENT.getCode(), settlement.getId(), settlement.getSettleNo(),
                fromStatus, SettlementStatusEnum.SETTLE_FAILED.getCode(),
                "链上回执失败: " + record.getFailReason(), null, null);
    }
}
