package com.lifechain.work.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.ClaimStatusEnum;
import com.lifechain.common.enums.WorkStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.work.entity.ClaimApplicationEntity;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.ClaimApplicationMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimChainReceiptHandler implements ChainReceiptHandler {

    private final ClaimApplicationMapper claimApplicationMapper;
    private final WorkMapper workMapper;
    private final AuditService auditService;

    @Override
    public String getBizType() {
        return BizTypeEnum.CLAIM.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        ClaimApplicationEntity claim = claimApplicationMapper.selectById(record.getBizId());
        if (claim == null) {
            log.warn("确权回执处理失败，未找到确权记录，bizId={}", record.getBizId());
            return;
        }
        String fromStatus = claim.getStatus();
        if (!ClaimStatusEnum.CLAIM_APPROVED_PENDING_CHAIN.getCode().equals(fromStatus)
                || !ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(claim.getChainStatus())) {
            log.info("确权当前状态不需要回执处理，claimNo={}, status={}", claim.getClaimNo(), fromStatus);
            return;
        }
        claim.setStatus(ClaimStatusEnum.CLAIM_SUCCESS.getCode());
        claim.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        claim.setChainConfirmTime(DateTimeUtil.nowUtc());
        claim.setTxHash(record.getTxHash());
        claim.setBlockHeight(record.getBlockHeight());
        claimApplicationMapper.updateById(claim);

        // 联动更新作品状态为 OWNERSHIP_CONFIRMED
        WorkEntity work = workMapper.selectById(claim.getWorkId());
        if (work != null) {
            String workFromStatus = work.getStatus();
            work.setStatus(WorkStatusEnum.OWNERSHIP_CONFIRMED.getCode());
            workMapper.updateById(work);
            auditService.writeStatusHistory(BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                    workFromStatus, WorkStatusEnum.OWNERSHIP_CONFIRMED.getCode(),
                    "确权链上回执确认，作品确权生效", null, null);
        }

        auditService.writeStatusHistory(BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                fromStatus, ClaimStatusEnum.CLAIM_SUCCESS.getCode(),
                "链上回执确认成功", null, null);
        auditService.writeAuditLog(BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                "CHAIN_RECEIPT_SUCCESS", "确权链上回执确认，txHash=" + record.getTxHash(),
                null, "SYSTEM", null, "SUCCESS", null);
        log.info("确权回执处理完成，claimNo={} -> CLAIM_SUCCESS", claim.getClaimNo());
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        ClaimApplicationEntity claim = claimApplicationMapper.selectById(record.getBizId());
        if (claim == null) return;
        String fromStatus = claim.getStatus();
        claim.setStatus(ClaimStatusEnum.CLAIM_CHAIN_FAILED.getCode());
        claim.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        claim.setFailReason(record.getFailReason());
        claimApplicationMapper.updateById(claim);

        // 联动回退作品状态
        WorkEntity work = workMapper.selectById(claim.getWorkId());
        if (work != null) {
            String workFromStatus = work.getStatus();
            work.setStatus(WorkStatusEnum.CLAIM_FAILED.getCode());
            workMapper.updateById(work);
            auditService.writeStatusHistory(BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                    workFromStatus, WorkStatusEnum.CLAIM_FAILED.getCode(),
                    "确权链上回执失败，作品确权失败", null, null);
        }

        auditService.writeStatusHistory(BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                fromStatus, ClaimStatusEnum.CLAIM_CHAIN_FAILED.getCode(),
                "链上回执失败: " + record.getFailReason(), null, null);
    }
}
