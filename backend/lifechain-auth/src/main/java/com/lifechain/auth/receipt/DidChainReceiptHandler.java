package com.lifechain.auth.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.entity.DidRecordEntity;
import com.lifechain.auth.mapper.DidRecordMapper;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.DidStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DidChainReceiptHandler implements ChainReceiptHandler {

    private final DidRecordMapper didRecordMapper;
    private final AuditService auditService;

    @Override
    public String getBizType() {
        return BizTypeEnum.DID.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        DidRecordEntity did = didRecordMapper.selectById(record.getBizId());
        if (did == null) {
            log.warn("DID回执处理失败，未找到DID记录，bizId={}", record.getBizId());
            return;
        }
        String fromStatus = did.getStatus();
        String chainStatus = did.getChainStatus();

        // 仅处理待回执状态
        if (!ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(chainStatus)) {
            log.info("DID当前链上状态不需要回执处理，didNo={}, chainStatus={}", did.getDidNo(), chainStatus);
            return;
        }

        String toStatus;
        switch (fromStatus) {
            case "DID_APPROVED_PENDING_CHAIN" -> {
                // 注册成功
                toStatus = DidStatusEnum.DID_ACTIVE.getCode();
                did.setActiveTime(DateTimeUtil.nowUtc());
            }
            case "DID_SUSPEND_PENDING_CHAIN" -> {
                // 挂起成功
                toStatus = DidStatusEnum.DID_SUSPENDED.getCode();
                did.setSuspendTime(DateTimeUtil.nowUtc());
            }
            case "DID_REVOKE_PENDING_CHAIN" -> {
                // 吊销成功
                toStatus = DidStatusEnum.DID_REVOKED.getCode();
                did.setRevokeTime(DateTimeUtil.nowUtc());
            }
            default -> {
                log.info("DID当前业务状态不需要回执处理，didNo={}, status={}", did.getDidNo(), fromStatus);
                return;
            }
        }

        did.setStatus(toStatus);
        did.setPreviousStatus(null);
        did.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        did.setTxHash(record.getTxHash());
        did.setBlockHeight(record.getBlockHeight());
        didRecordMapper.updateById(did);

        auditService.writeStatusHistory(BizTypeEnum.DID.getCode(), did.getId(), did.getDidNo(),
                fromStatus, toStatus,
                "链上回执确认成功", null, null);
        auditService.writeAuditLog(BizTypeEnum.DID.getCode(), did.getId(), did.getDidNo(),
                "CHAIN_RECEIPT_SUCCESS", "DID链上回执确认，txHash=" + record.getTxHash(),
                null, "SYSTEM", null, "SUCCESS", null);
        log.info("DID回执处理完成，didNo={} -> {}", did.getDidNo(), toStatus);
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        DidRecordEntity did = didRecordMapper.selectById(record.getBizId());
        if (did == null) return;
        String fromStatus = did.getStatus();

        // 根据失败前的中间态回滚到对应的原始状态
        String rollbackStatus;
        switch (fromStatus) {
            case "DID_APPROVED_PENDING_CHAIN" -> rollbackStatus = DidStatusEnum.DID_CHAIN_FAILED.getCode();
            case "DID_SUSPEND_PENDING_CHAIN", "DID_REVOKE_PENDING_CHAIN" ->
                    rollbackStatus = did.getPreviousStatus() != null && !did.getPreviousStatus().isBlank()
                            ? did.getPreviousStatus()
                            : DidStatusEnum.DID_ACTIVE.getCode();
            default -> rollbackStatus = DidStatusEnum.DID_CHAIN_FAILED.getCode();
        }

        did.setStatus(rollbackStatus);
        did.setPreviousStatus(null);
        did.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        did.setFailReason(record.getFailReason());
        didRecordMapper.updateById(did);

        auditService.writeStatusHistory(BizTypeEnum.DID.getCode(), did.getId(), did.getDidNo(),
                fromStatus, rollbackStatus,
                "链上回执失败: " + record.getFailReason(), null, null);
    }
}
