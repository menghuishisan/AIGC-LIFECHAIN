package com.lifechain.regulator.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.FreezeStatusEnum;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.infra.notification.NotificationService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.regulator.dto.UnfreezeRequest;
import com.lifechain.regulator.entity.FreezeRecordEntity;
import com.lifechain.regulator.mapper.FreezeRecordMapper;
import com.lifechain.regulator.service.FreezeService;
import com.lifechain.regulator.service.FreezeTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FreezeChainReceiptHandler implements ChainReceiptHandler {

    private final FreezeRecordMapper freezeRecordMapper;
    private final AuditService auditService;
    private final FreezeTargetService freezeTargetService;
    private final FreezeService freezeService;
    private final NotificationService notificationService;
    private final TraceEventService traceEventService;

    @Override
    public String getBizType() {
        return BizTypeEnum.FREEZE.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        FreezeRecordEntity freeze = freezeRecordMapper.selectById(record.getBizId());
        if (freeze == null) {
            log.warn("冻结回执处理失败，未找到冻结记录，bizId={}", record.getBizId());
            return;
        }
        String fromChainStatus = freeze.getChainStatus();
        if (ChainStatusEnum.CHAIN_SUCCESS.getCode().equals(fromChainStatus)) {
            return;
        }

        String fromFreezeStatus = freeze.getFreezeStatus();
        freeze.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        freeze.setTxHash(record.getTxHash());
        freeze.setBlockHeight(record.getBlockHeight());

        // 根据当前冻结状态区分冻结回执和解冻回执
        if (FreezeStatusEnum.FREEZE_APPROVED_PENDING_CHAIN.getCode().equals(fromFreezeStatus)) {
            // 冻结回执成功：推进到 FREEZE_APPROVED 并联动冻结目标对象
            freeze.setFreezeStatus(FreezeStatusEnum.FREEZE_APPROVED.getCode());
            freeze.setEffectiveTime(DateTimeUtil.nowUtc());
            freezeRecordMapper.updateById(freeze);

            // 链上确认后才联动冻结目标对象
            try {
                freezeTargetService.freezeTarget(freeze.getTargetType(), freeze.getTargetNo(), freeze.getFreezeNo());
                log.info("冻结回执联动冻结目标成功，freezeNo={}", freeze.getFreezeNo());
            } catch (Exception e) {
                log.error("冻结回执联动冻结目标失败，freezeNo={}", freeze.getFreezeNo(), e);
            }

            auditService.writeStatusHistory(BizTypeEnum.FREEZE.getCode(), freeze.getId(), freeze.getFreezeNo(),
                    fromFreezeStatus, FreezeStatusEnum.FREEZE_APPROVED.getCode(),
                    "冻结链上回执确认成功，冻结生效", null, null);

            if (!"REVIEW_REJECTED_PENDING_UNFREEZE".equals(freeze.getReviewStatus())) {
                Long notifyAccountId = freezeTargetService.resolveTargetAccountId(freeze.getTargetType(), freeze.getTargetNo());
                if (notifyAccountId != null) {
                    notificationService.sendNotice(notifyAccountId, "冻结通知",
                            "您的" + freeze.getTargetType() + "已被冻结，冻结编号: " + freeze.getFreezeNo() + "，原因: " + freeze.getFreezeReason(),
                            "FREEZE", BizTypeEnum.FREEZE.getCode(), freeze.getFreezeNo());
                }
                traceEventService.writeTraceEvent(BizTypeEnum.FREEZE.getCode(), freeze.getId(), freeze.getFreezeNo(),
                        "FREEZE_EFFECTIVE", "冻结链上回执确认，冻结生效", null, "SYSTEM", null);
            } else {
                try {
                    UnfreezeRequest request = new UnfreezeRequest();
                    request.setFreezeNo(freeze.getFreezeNo());
                    request.setUnfreezeReason(freeze.getUnfreezeReason());
                    request.setRequestId(BizNoUtil.generate("REQ"));
                    freezeService.unfreeze(null, request);
                } catch (Exception e) {
                    log.error("冻结复核驳回后的自动解冻提交失败，freezeNo={}", freeze.getFreezeNo(), e);
                }
            }
        } else if (FreezeStatusEnum.UNFREEZE_PENDING_CHAIN.getCode().equals(fromFreezeStatus)) {
            // 解冻回执成功：推进到 UNFREEZE_APPROVED 并联动恢复目标对象
            freeze.setFreezeStatus(FreezeStatusEnum.UNFREEZE_APPROVED.getCode());
            freeze.setUnfreezeTime(DateTimeUtil.nowUtc());
            freezeRecordMapper.updateById(freeze);

            // 链上确认后才联动解冻目标对象
            try {
                freezeTargetService.unfreezeTarget(
                        freeze.getTargetType(),
                        freeze.getTargetNo(),
                        freeze.getPreviousTargetStatus(),
                        freeze.getFreezeNo());
                log.info("解冻回执联动恢复目标成功，freezeNo={}", freeze.getFreezeNo());
            } catch (Exception e) {
                log.error("解冻回执联动恢复目标失败，freezeNo={}", freeze.getFreezeNo(), e);
            }

            auditService.writeStatusHistory(BizTypeEnum.FREEZE.getCode(), freeze.getId(), freeze.getFreezeNo(),
                    fromFreezeStatus, FreezeStatusEnum.UNFREEZE_APPROVED.getCode(),
                    "解冻链上回执确认成功，解冻生效", null, null);

            Long notifyAccountId = freezeTargetService.resolveTargetAccountId(freeze.getTargetType(), freeze.getTargetNo());
            if (notifyAccountId != null) {
                notificationService.sendNotice(notifyAccountId, "解冻通知",
                        "您的" + freeze.getTargetType() + "已解冻，冻结编号: " + freeze.getFreezeNo() + "，原因: " + freeze.getUnfreezeReason(),
                        "UNFREEZE", BizTypeEnum.FREEZE.getCode(), freeze.getFreezeNo());
            }
            traceEventService.writeTraceEvent(BizTypeEnum.FREEZE.getCode(), freeze.getId(), freeze.getFreezeNo(),
                    "UNFREEZE_COMPLETED", "解冻链上回执确认，解冻完成", null, "SYSTEM", null);
        } else {
            // 其他状态仅更新链上状态
            freezeRecordMapper.updateById(freeze);
            auditService.writeStatusHistory(BizTypeEnum.FREEZE.getCode(), freeze.getId(), freeze.getFreezeNo(),
                    fromFreezeStatus, fromFreezeStatus,
                    "链上回执确认成功", null, null);
        }

        auditService.writeAuditLog(BizTypeEnum.FREEZE.getCode(), freeze.getId(), freeze.getFreezeNo(),
                "CHAIN_RECEIPT_SUCCESS", "冻结/解冻链上回执确认，txHash=" + record.getTxHash(),
                null, "SYSTEM", null, "SUCCESS", null);
        log.info("冻结回执处理完成，freezeNo={}, status={}", freeze.getFreezeNo(), freeze.getFreezeStatus());
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        FreezeRecordEntity freeze = freezeRecordMapper.selectById(record.getBizId());
        if (freeze == null) return;

        String fromFreezeStatus = freeze.getFreezeStatus();
        freeze.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());

        // 冻结失败时回滚到 FREEZE_APPLIED
        if (FreezeStatusEnum.FREEZE_APPROVED_PENDING_CHAIN.getCode().equals(fromFreezeStatus)) {
            if ("REVIEW_REJECTED_PENDING_UNFREEZE".equals(freeze.getReviewStatus())) {
                freeze.setFreezeStatus(FreezeStatusEnum.UNFREEZE_APPROVED.getCode());
                freeze.setUnfreezeTime(DateTimeUtil.nowUtc());
            } else {
                freeze.setFreezeStatus(FreezeStatusEnum.FREEZE_APPLIED.getCode());
            }
        }
        // 解冻失败时回滚到 FREEZE_APPROVED
        else if (FreezeStatusEnum.UNFREEZE_PENDING_CHAIN.getCode().equals(fromFreezeStatus)) {
            freeze.setFreezeStatus(FreezeStatusEnum.FREEZE_APPROVED.getCode());
        }

        freezeRecordMapper.updateById(freeze);

        auditService.writeStatusHistory(BizTypeEnum.FREEZE.getCode(), freeze.getId(), freeze.getFreezeNo(),
                fromFreezeStatus, freeze.getFreezeStatus(),
                "链上回执失败: " + record.getFailReason(), null, null);
    }
}
