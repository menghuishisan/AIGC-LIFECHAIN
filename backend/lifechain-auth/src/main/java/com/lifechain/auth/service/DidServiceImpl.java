package com.lifechain.auth.service;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.dto.DidApplyRequest;
import com.lifechain.auth.dto.DidInfoVO;
import com.lifechain.auth.dto.DidReviewRequest;
import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.entity.DidRecordEntity;
import com.lifechain.auth.entity.SubjectProfileEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.auth.mapper.DidRecordMapper;
import com.lifechain.auth.mapper.SubjectProfileMapper;
import com.lifechain.chain.adapter.DidChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.*;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DID（数字身份）服务实现
 * <p>
 * 实现DID的申请、审核（含Fabric上链）、查询、挂起、吊销等完整生命周期管理。
 * 审核通过后自动调用链码适配器完成上链操作，并根据链上结果更新业务状态。
 * 所有状态变更均写入状态变更历史，关键操作写入审计日志。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DidServiceImpl implements DidService {

    private final DidRecordMapper didRecordMapper;
    private final AccountMapper accountMapper;
    private final SubjectProfileMapper subjectProfileMapper;
    private final DidChainAdapter didChainAdapter;
    private final AuditService auditService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyDid(Long accountId, DidApplyRequest request) {
        log.info("申请DID，accountId={}", accountId);

        // 校验账户认证状态
        AccountEntity account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BizException(ErrorCodeEnum.ACCOUNT_NOT_FOUND, "账户不存在");
        }
        if (!AccountStatusEnum.AUTH_APPROVED.getCode().equals(account.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "账户未通过实名认证，无法申请DID", null, account.getStatus());
        }

        // 校验不存在已激活或审核中的DID
        DidRecordEntity existingDid = didRecordMapper.selectByAccountId(accountId);
        if (existingDid != null) {
            String didStatus = existingDid.getStatus();
            if (DidStatusEnum.DID_ACTIVE.getCode().equals(didStatus)
                    || DidStatusEnum.DID_PENDING.getCode().equals(didStatus)
                    || DidStatusEnum.DID_APPROVED_PENDING_CHAIN.getCode().equals(didStatus)) {
                throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                        "已存在有效的DID或DID正在审核中", null, didStatus);
            }
        }

        // 查找主体信息
        SubjectProfileEntity subject = subjectProfileMapper.selectByAccountId(accountId);
        if (subject == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "主体信息不存在");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        // 创建DID记录
        DidRecordEntity didRecord = new DidRecordEntity();
        didRecord.setDidNo(BizNoUtil.didNo());
        didRecord.setDidValue("did:lifechain:" + account.getAccountNo());
        didRecord.setAccountId(accountId);
        didRecord.setSubjectId(subject.getId());
        didRecord.setStatus(DidStatusEnum.DID_PENDING.getCode());
        didRecord.setChainStatus("");
        didRecord.setApplyTime(now);
        didRecordMapper.insert(didRecord);

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                "", DidStatusEnum.DID_PENDING.getCode(),
                "用户申请DID", null, accountId);

        log.info("DID申请成功，didNo={}, didValue={}", didRecord.getDidNo(), didRecord.getDidValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewDid(Long reviewerId, DidReviewRequest request) {
        log.info("审核DID，didNo={}, reviewResult={}", request.getDidNo(), request.getReviewResult());

        DidRecordEntity didRecord = didRecordMapper.selectByDidNo(request.getDidNo());
        if (didRecord == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "DID记录不存在");
        }

        // 校验状态
        if (!DidStatusEnum.DID_PENDING.getCode().equals(didRecord.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前DID状态不允许审核", null, didRecord.getStatus());
        }

        ReviewResultEnum reviewResult = ReviewResultEnum.fromCode(request.getReviewResult());
        LocalDateTime now = DateTimeUtil.nowUtc();
        String fromStatus = didRecord.getStatus();

        didRecord.setReviewerId(reviewerId);
        didRecord.setReviewComment(request.getReviewComment());
        didRecord.setReasonCode(request.getReasonCode());
        didRecord.setApproveTime(now);

        if (ReviewResultEnum.APPROVED == reviewResult) {
            // 审核通过：先标记为待上链
            didRecord.setStatus(DidStatusEnum.DID_APPROVED_PENDING_CHAIN.getCode());
            didRecord.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
            didRecordMapper.updateById(didRecord);

            auditService.writeStatusHistory(
                    BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                    fromStatus, DidStatusEnum.DID_APPROVED_PENDING_CHAIN.getCode(),
                    "DID审核通过，准备上链", request.getReasonCode(), reviewerId);

            // 获取账户信息用于上链
            AccountEntity account = accountMapper.selectById(didRecord.getAccountId());
            SubjectProfileEntity subject = subjectProfileMapper.selectByAccountId(didRecord.getAccountId());

            // 调用链码适配器上链
            try {
                ChainSubmitResult chainResult = didChainAdapter.registerDid(
                        didRecord.getId(), didRecord.getDidNo(), didRecord.getDidValue(),
                        account.getAccountNo(), subject.getSubjectType(), now);

                if (chainResult.isSuccess()) {
                    // 链提交成功 → 标记为 CHAIN_SUBMITTED，等待回执确认后再推进到 DID_ACTIVE
                    didRecord.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                    didRecord.setTxHash(chainResult.getTxHash());
                    didRecord.setBlockHeight(chainResult.getBlockHeight());
                    didRecordMapper.updateById(didRecord);

                    auditService.writeStatusHistory(
                            BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                            DidStatusEnum.DID_APPROVED_PENDING_CHAIN.getCode(), DidStatusEnum.DID_APPROVED_PENDING_CHAIN.getCode(),
                            "DID链上交易已提交，等待回执确认", null, reviewerId);

                    log.info("DID链上交易已提交待回执，didNo={}, txHash={}", didRecord.getDidNo(), chainResult.getTxHash());
                } else {
                    didRecord.setStatus(DidStatusEnum.DID_CHAIN_FAILED.getCode());
                    didRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                    didRecord.setFailReason(chainResult.getFailReason());
                    didRecordMapper.updateById(didRecord);

                    auditService.writeStatusHistory(
                            BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                            DidStatusEnum.DID_APPROVED_PENDING_CHAIN.getCode(), DidStatusEnum.DID_CHAIN_FAILED.getCode(),
                            "DID上链失败：" + chainResult.getFailReason(), chainResult.getReasonCode(), reviewerId);

                    log.warn("DID上链失败，didNo={}, 原因={}", didRecord.getDidNo(), chainResult.getFailReason());
                }
            } catch (Exception e) {
                didRecord.setStatus(DidStatusEnum.DID_CHAIN_FAILED.getCode());
                didRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                didRecord.setFailReason(e.getMessage());
                didRecordMapper.updateById(didRecord);

                auditService.writeStatusHistory(
                        BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                        DidStatusEnum.DID_APPROVED_PENDING_CHAIN.getCode(), DidStatusEnum.DID_CHAIN_FAILED.getCode(),
                        "DID上链异常：" + e.getMessage(), null, reviewerId);

                log.error("DID上链异常，didNo={}", didRecord.getDidNo(), e);
            }
        } else {
            // 审核驳回
            didRecord.setStatus(DidStatusEnum.DID_NOT_APPLIED.getCode());
            didRecordMapper.updateById(didRecord);

            auditService.writeStatusHistory(
                    BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                    fromStatus, DidStatusEnum.DID_NOT_APPLIED.getCode(),
                    "DID审核驳回：" + request.getReviewComment(), request.getReasonCode(), reviewerId);

            log.info("DID审核驳回，didNo={}", didRecord.getDidNo());
        }

        // 写入审计日志
        auditService.writeAuditLog(
                BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                "DID_REVIEW", "DID审核结果=" + reviewResult.getDescription() + "，意见=" + request.getReviewComment(),
                reviewerId, RoleEnum.PLATFORM_ADMIN.getCode(), null,
                "SUCCESS", request.getReasonCode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DidInfoVO getDidInfo(String didNo, Long accountId) {
        log.info("查询DID信息，didNo={}, accountId={}", didNo, accountId);

        DidRecordEntity didRecord = didRecordMapper.selectByDidNo(didNo);
        if (didRecord == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "DID记录不存在");
        }
        // 用户侧仅允许查看自己的DID
        if (!didRecord.getAccountId().equals(accountId)) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看他人DID信息");
        }

        return DidInfoVO.builder()
                .didNo(didRecord.getDidNo())
                .didValue(didRecord.getDidValue())
                .status(didRecord.getStatus())
                .chainStatus(didRecord.getChainStatus())
                .activeTime(DateTimeUtil.formatUtc(didRecord.getActiveTime()))
                .applyTime(DateTimeUtil.formatUtc(didRecord.getApplyTime()))
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<DidInfoVO> listDids(String status, String accountNo, PageQuery query) {
        log.info("管理员查询DID列表，status={}, accountNo={}", status, accountNo);

        // 如果指定了accountNo，先查找accountId
        Long accountId = null;
        if (accountNo != null && !accountNo.isBlank()) {
            AccountEntity account = accountMapper.selectByAccountNo(accountNo);
            if (account == null) {
                return PageResult.of(java.util.List.of(), 0L, query.getPageNo(), query.getPageSize());
            }
            accountId = account.getId();
        }

        Page<DidRecordEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<DidRecordEntity> wrapper = new LambdaQueryWrapper<DidRecordEntity>()
                .eq(status != null && !status.isBlank(), DidRecordEntity::getStatus, status)
                .eq(accountId != null, DidRecordEntity::getAccountId, accountId)
                .orderByDesc(DidRecordEntity::getApplyTime);

        Page<DidRecordEntity> result = didRecordMapper.selectPage(page, wrapper);

        java.util.List<DidInfoVO> voList = result.getRecords().stream()
                .map(record -> DidInfoVO.builder()
                        .didNo(record.getDidNo())
                        .didValue(record.getDidValue())
                        .status(record.getStatus())
                        .chainStatus(record.getChainStatus())
                        .activeTime(DateTimeUtil.formatUtc(record.getActiveTime()))
                        .applyTime(DateTimeUtil.formatUtc(record.getApplyTime()))
                        .build())
                .toList();

        return PageResult.of(voList, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void suspendDid(Long operatorId, String didNo, String reason) {
        log.info("挂起DID，didNo={}", didNo);

        DidRecordEntity didRecord = didRecordMapper.selectByDidNo(didNo);
        if (didRecord == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "DID记录不存在");
        }

        if (!DidStatusEnum.DID_ACTIVE.getCode().equals(didRecord.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前DID状态不允许挂起", null, didRecord.getStatus());
        }

        String fromStatus = didRecord.getStatus();

        // 先设置中间态：挂起待上链确认
        didRecord.setPreviousStatus(fromStatus);
        didRecord.setStatus(DidStatusEnum.DID_SUSPEND_PENDING_CHAIN.getCode());
        didRecord.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        didRecordMapper.updateById(didRecord);

        auditService.writeStatusHistory(
                BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                fromStatus, DidStatusEnum.DID_SUSPEND_PENDING_CHAIN.getCode(),
                "DID挂起申请，准备上链：" + reason, null, operatorId);

        // 调用链码适配器挂起
        try {
            ChainSubmitResult chainResult = didChainAdapter.suspendDid(didRecord.getId(), didNo, reason);
            if (chainResult.isSuccess()) {
                // 链提交成功 → CHAIN_SUBMITTED，等待回执确认后再推进到 DID_SUSPENDED
                didRecord.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                didRecord.setTxHash(chainResult.getTxHash());
                didRecordMapper.updateById(didRecord);
                log.info("DID挂起链上交易已提交待回执，didNo={}, txHash={}", didNo, chainResult.getTxHash());
            } else {
                didRecord.setStatus(DidStatusEnum.DID_ACTIVE.getCode());
                didRecord.setPreviousStatus(null);
                didRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                didRecord.setFailReason(chainResult.getFailReason());
                didRecordMapper.updateById(didRecord);

                auditService.writeStatusHistory(
                        BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                        DidStatusEnum.DID_SUSPEND_PENDING_CHAIN.getCode(), DidStatusEnum.DID_ACTIVE.getCode(),
                        "DID挂起上链失败，回滚：" + chainResult.getFailReason(), null, operatorId);
                throw new BizException(ErrorCodeEnum.CHAIN_SUBMIT_FAILED,
                        "DID挂起上链失败：" + chainResult.getFailReason());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            didRecord.setStatus(DidStatusEnum.DID_ACTIVE.getCode());
            didRecord.setPreviousStatus(null);
            didRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            didRecord.setFailReason(e.getMessage());
            didRecordMapper.updateById(didRecord);

            auditService.writeStatusHistory(
                    BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                    DidStatusEnum.DID_SUSPEND_PENDING_CHAIN.getCode(), DidStatusEnum.DID_ACTIVE.getCode(),
                    "DID挂起上链异常，回滚：" + e.getMessage(), null, operatorId);
            throw new BizException(ErrorCodeEnum.CHAIN_SUBMIT_FAILED,
                    "DID挂起上链异常：" + e.getMessage());
        }

        // 写入审计日志
        auditService.writeAuditLog(
                BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                "DID_SUSPEND", "挂起DID，原因=" + reason,
                operatorId, RoleEnum.PLATFORM_ADMIN.getCode(), null,
                "SUCCESS", null);

        log.info("DID挂起链上交易已提交，didNo={}", didNo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeDid(Long operatorId, String didNo, String reason) {
        log.info("吊销DID，didNo={}", didNo);

        DidRecordEntity didRecord = didRecordMapper.selectByDidNo(didNo);
        if (didRecord == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "DID记录不存在");
        }

        String currentStatus = didRecord.getStatus();
        if (!DidStatusEnum.DID_ACTIVE.getCode().equals(currentStatus)
                && !DidStatusEnum.DID_SUSPENDED.getCode().equals(currentStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前DID状态不允许吊销", null, currentStatus);
        }

        // 先设置中间态：吊销待上链确认
        didRecord.setPreviousStatus(currentStatus);
        didRecord.setStatus(DidStatusEnum.DID_REVOKE_PENDING_CHAIN.getCode());
        didRecord.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        didRecordMapper.updateById(didRecord);

        auditService.writeStatusHistory(
                BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                currentStatus, DidStatusEnum.DID_REVOKE_PENDING_CHAIN.getCode(),
                "DID吊销申请，准备上链：" + reason, null, operatorId);

        // 调用链码适配器吊销
        try {
            ChainSubmitResult chainResult = didChainAdapter.revokeDid(didRecord.getId(), didNo, reason);
            if (chainResult.isSuccess()) {
                // 链提交成功 → CHAIN_SUBMITTED，等待回执确认后再推进到 DID_REVOKED
                didRecord.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                didRecord.setTxHash(chainResult.getTxHash());
                didRecordMapper.updateById(didRecord);
                log.info("DID吊销链上交易已提交待回执，didNo={}, txHash={}", didNo, chainResult.getTxHash());
            } else {
                // 吊销上链失败，回滚到原状态
                didRecord.setStatus(currentStatus);
                didRecord.setPreviousStatus(null);
                didRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                didRecord.setFailReason(chainResult.getFailReason());
                didRecordMapper.updateById(didRecord);

                auditService.writeStatusHistory(
                        BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                        DidStatusEnum.DID_REVOKE_PENDING_CHAIN.getCode(), currentStatus,
                        "DID吊销上链失败，回滚：" + chainResult.getFailReason(), null, operatorId);
                throw new BizException(ErrorCodeEnum.CHAIN_SUBMIT_FAILED,
                        "DID吊销上链失败：" + chainResult.getFailReason());
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            didRecord.setStatus(currentStatus);
            didRecord.setPreviousStatus(null);
            didRecord.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            didRecord.setFailReason(e.getMessage());
            didRecordMapper.updateById(didRecord);

            auditService.writeStatusHistory(
                    BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                    DidStatusEnum.DID_REVOKE_PENDING_CHAIN.getCode(), currentStatus,
                    "DID吊销上链异常，回滚：" + e.getMessage(), null, operatorId);
            throw new BizException(ErrorCodeEnum.CHAIN_SUBMIT_FAILED,
                    "DID吊销上链异常：" + e.getMessage());
        }

        // 写入审计日志
        auditService.writeAuditLog(
                BizTypeEnum.DID.getCode(), didRecord.getId(), didRecord.getDidNo(),
                "DID_REVOKE", "吊销DID，原因=" + reason,
                operatorId, RoleEnum.PLATFORM_ADMIN.getCode(), null,
                "SUCCESS", null);

        log.info("DID吊销链上交易已提交，didNo={}", didNo);
    }
}
