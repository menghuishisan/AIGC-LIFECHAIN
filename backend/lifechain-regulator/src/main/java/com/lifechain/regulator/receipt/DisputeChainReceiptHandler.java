package com.lifechain.regulator.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.DisputeStatusEnum;
import com.lifechain.regulator.entity.DisputeCaseEntity;
import com.lifechain.regulator.mapper.DisputeCaseMapper;
import com.lifechain.regulator.service.DisputeResolutionHandler;
import com.lifechain.regulator.service.FreezeService;
import com.lifechain.regulator.entity.FreezeRecordEntity;
import com.lifechain.regulator.mapper.FreezeRecordMapper;
import com.lifechain.regulator.dto.UnfreezeRequest;
import com.lifechain.infra.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DisputeChainReceiptHandler implements ChainReceiptHandler {

    private final DisputeCaseMapper disputeCaseMapper;
    private final AuditService auditService;
    private final FreezeService freezeService;
    private final FreezeRecordMapper freezeRecordMapper;
    private final NotificationService notificationService;
    private final DisputeResolutionHandler disputeResolutionHandler;

    /** 待链上确认 → 终态的映射 */
    private static final Map<String, String> PENDING_TO_FINAL = Map.of(
            DisputeStatusEnum.DISPUTE_RESOLVED_PENDING_CHAIN.getCode(), DisputeStatusEnum.DISPUTE_RESOLVED.getCode(),
            DisputeStatusEnum.DISPUTE_REJECTED_PENDING_CHAIN.getCode(), DisputeStatusEnum.DISPUTE_REJECTED.getCode(),
            DisputeStatusEnum.DISPUTE_CLOSED_PENDING_CHAIN.getCode(), DisputeStatusEnum.DISPUTE_CLOSED.getCode()
    );

    @Override
    public String getBizType() {
        return BizTypeEnum.DISPUTE.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        DisputeCaseEntity dispute = disputeCaseMapper.selectById(record.getBizId());
        if (dispute == null) {
            log.warn("争议回执处理失败，未找到争议记录，bizId={}", record.getBizId());
            return;
        }
        String fromChainStatus = dispute.getChainStatus();
        if (ChainStatusEnum.CHAIN_SUCCESS.getCode().equals(fromChainStatus)) {
            return;
        }

        String fromStatus = dispute.getStatus();
        String finalStatus = PENDING_TO_FINAL.get(fromStatus);

        dispute.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        dispute.setTxHash(record.getTxHash());
        dispute.setBlockHeight(record.getBlockHeight());

        if (finalStatus != null) {
            // 从待链上确认状态推进到终态
            dispute.setStatus(finalStatus);
            disputeCaseMapper.updateById(dispute);

            auditService.writeStatusHistory(BizTypeEnum.DISPUTE.getCode(), dispute.getId(), dispute.getCaseNo(),
                    fromStatus, finalStatus,
                    "争议结论链上回执确认成功，进入终态", null, null);

            // 链上确认成功后才执行联动动作
            handleDisputeClosingActions(dispute, finalStatus);
        } else {
            disputeCaseMapper.updateById(dispute);
            auditService.writeStatusHistory(BizTypeEnum.DISPUTE.getCode(), dispute.getId(), dispute.getCaseNo(),
                    fromStatus, fromStatus,
                    "争议结论链上回执确认成功", null, null);
        }

        auditService.writeAuditLog(BizTypeEnum.DISPUTE.getCode(), dispute.getId(), dispute.getCaseNo(),
                "CHAIN_RECEIPT_SUCCESS", "争议结论链上回执确认，txHash=" + record.getTxHash(),
                null, "SYSTEM", null, "SUCCESS", null);
        log.info("争议回执处理完成，caseNo={}, status={}", dispute.getCaseNo(), dispute.getStatus());
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        DisputeCaseEntity dispute = disputeCaseMapper.selectById(record.getBizId());
        if (dispute == null) return;
        dispute.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        disputeCaseMapper.updateById(dispute);

        auditService.writeStatusHistory(BizTypeEnum.DISPUTE.getCode(), dispute.getId(), dispute.getCaseNo(),
                dispute.getStatus(), dispute.getStatus(),
                "争议结论链上回执失败: " + record.getFailReason(), null, null);
    }

    /**
     * 争议结论联动：解除关联冻结、通知双方，争议解决时联动退款、逆分账、授权撤销
     */
    private void handleDisputeClosingActions(DisputeCaseEntity caseEntity, String conclusion) {
        // 1. 解除关联订单/作品的活跃冻结
        releaseRelatedFreezes(caseEntity);

        // 2. 争议解决（申请方胜诉）→ 联动退款、逆分账、授权撤销
        if (DisputeStatusEnum.DISPUTE_RESOLVED.getCode().equals(conclusion)
                && caseEntity.getOrderNo() != null) {
            try {
                disputeResolutionHandler.handleDisputeResolved(
                        caseEntity.getOrderNo(), caseEntity.getCaseNo(), null);
            } catch (Exception e) {
                log.warn("争议结论联动处理异常，caseNo={}", caseEntity.getCaseNo(), e);
            }
        }

        // 3. 通知申请人
        try {
            notificationService.sendNotice(caseEntity.getApplicantAccountId(), "争议结案通知",
                    "争议案件 " + caseEntity.getCaseNo() + " 结论：" + conclusion,
                    "DISPUTE", BizTypeEnum.DISPUTE.getCode(), caseEntity.getCaseNo());
        } catch (Exception e) {
            log.warn("发送争议通知失败", e);
        }

        // 4. 通知被申请人
        if (caseEntity.getRespondentAccountId() != null) {
            try {
                notificationService.sendNotice(caseEntity.getRespondentAccountId(), "争议结案通知",
                        "争议案件 " + caseEntity.getCaseNo() + " 结论：" + conclusion,
                        "DISPUTE", BizTypeEnum.DISPUTE.getCode(), caseEntity.getCaseNo());
            } catch (Exception e) {
                log.warn("发送争议通知失败", e);
            }
        }
    }

    /**
     * 解除争议关联的活跃冻结记录
     */
    private void releaseRelatedFreezes(DisputeCaseEntity caseEntity) {
        releaseFreezesForTarget("ORDER", caseEntity.getOrderNo(), caseEntity.getCaseNo());
        releaseFreezesForTarget("WORK", caseEntity.getWorkNo(), caseEntity.getCaseNo());
    }

    private void releaseFreezesForTarget(String targetType, String targetNo, String caseNo) {
        if (targetNo == null) return;
        List<FreezeRecordEntity> freezes = freezeRecordMapper.selectActiveFreezeByTargetNo(targetType, targetNo);
        for (FreezeRecordEntity freeze : freezes) {
            try {
                UnfreezeRequest req = new UnfreezeRequest();
                req.setFreezeNo(freeze.getFreezeNo());
                req.setUnfreezeReason("争议结案自动解冻: " + caseNo);
                freezeService.unfreeze(null, req);
            } catch (Exception e) {
                log.warn("争议结案解冻失败，caseNo={}, freezeNo={}", caseNo, freeze.getFreezeNo(), e);
            }
        }
    }
}
