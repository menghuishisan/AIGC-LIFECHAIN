package com.lifechain.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.auth.entity.DidRecordEntity;
import com.lifechain.auth.mapper.DidRecordMapper;
import com.lifechain.chain.adapter.ClaimChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.*;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.work.assembler.WorkVoAssembler;
import com.lifechain.work.dto.ClaimDetailVO;
import com.lifechain.work.dto.ClaimReviewRequest;
import com.lifechain.work.dto.ClaimSubmitRequest;
import com.lifechain.work.entity.ClaimApplicationEntity;
import com.lifechain.work.entity.ClaimReviewRecordEntity;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.ClaimApplicationMapper;
import com.lifechain.work.mapper.ClaimReviewRecordMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 确权服务实现
 * <p>
 * 实现确权申请提交、详情查询、审核及列表查询等功能。
 * 审核通过后调用链码完成确权上链，所有状态变更写入状态历史和审计日志。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final ClaimApplicationMapper claimApplicationMapper;
    private final ClaimReviewRecordMapper claimReviewRecordMapper;
    private final WorkMapper workMapper;
    private final DidRecordMapper didRecordMapper;
    private final ClaimChainAdapter claimChainAdapter;
    private final AuditService auditService;
    private final TraceEventService traceEventService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClaimDetailVO submitClaim(Long accountId, ClaimSubmitRequest request) {
        log.info("提交确权申请，accountId={}, workNo={}", accountId, request.getWorkNo());

        // 校验DID已生效
        DidRecordEntity didRecord = didRecordMapper.selectByAccountId(accountId);
        if (didRecord == null || !DidStatusEnum.DID_ACTIVE.getCode().equals(didRecord.getStatus())) {
            throw new BizException(ErrorCodeEnum.DID_NOT_ACTIVE, "请先完成DID认证");
        }

        // 校验作品存在且属于当前用户
        WorkEntity work = workMapper.selectByWorkNo(request.getWorkNo());
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }
        if (!work.getCreatorAccountId().equals(accountId)) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权操作此作品");
        }

        // 校验作品状态为可确权
        if (!WorkStatusEnum.READY_FOR_CLAIM.getCode().equals(work.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "作品当前状态不允许提交确权", null, work.getStatus());
        }

        // 校验无重复有效确权申请
        List<ClaimApplicationEntity> existingClaims = claimApplicationMapper.selectByWorkId(work.getId());
        boolean hasActiveClaim = existingClaims.stream()
                .anyMatch(c -> !ClaimStatusEnum.CLAIM_REJECTED.getCode().equals(c.getStatus())
                        && !ClaimStatusEnum.CLAIM_CANCELLED.getCode().equals(c.getStatus()));
        if (hasActiveClaim) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "该作品已存在有效的确权申请");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        // 创建确权申请
        ClaimApplicationEntity claim = new ClaimApplicationEntity();
        claim.setClaimNo(BizNoUtil.claimNo());
        claim.setWorkId(work.getId());
        claim.setWorkNo(work.getWorkNo());
        claim.setApplicantAccountId(accountId);
        claim.setApplicantDidId(didRecord.getId());
        claim.setStatus(ClaimStatusEnum.CLAIM_SUBMITTED.getCode());
        claim.setChainStatus("");
        claim.setSubmitTime(now);
        claimApplicationMapper.insert(claim);

        log.info("确权申请创建成功，claimNo={}, claimId={}", claim.getClaimNo(), claim.getId());

        // 更新作品状态为确权审核中
        String oldWorkStatus = work.getStatus();
        work.setStatus(WorkStatusEnum.CLAIM_REVIEWING.getCode());
        workMapper.updateById(work);

        // 写入状态变更历史（确权）
        auditService.writeStatusHistory(
                BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                "", ClaimStatusEnum.CLAIM_SUBMITTED.getCode(),
                "提交确权申请", null, accountId);

        // 写入状态变更历史（作品）
        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                oldWorkStatus, WorkStatusEnum.CLAIM_REVIEWING.getCode(),
                "确权申请已提交", null, accountId);

        // 写入审计日志
        auditService.writeAuditLog(
                BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                "SUBMIT", "提交确权申请，workNo=" + work.getWorkNo(),
                accountId, null, null, "SUCCESS", null);

        traceEventService.writeTraceEvent(BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                "CLAIM_SUBMITTED", "确权申请已提交", accountId, null, null);
        traceEventService.writeTraceEvent(BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "CLAIM_SUBMITTED", "关联确权申请已提交", accountId, null, null);

        return toClaimDetailVO(claim);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClaimDetailVO getClaimDetail(String claimNo, Long viewerAccountId) {
        ClaimApplicationEntity claim = claimApplicationMapper.selectByClaimNo(claimNo);
        if (claim == null) {
            throw new BizException(ErrorCodeEnum.CLAIM_NOT_FOUND);
        }
        // 归属校验：仅申请人或管理员/监管员可查看
        if (!claim.getApplicantAccountId().equals(viewerAccountId) && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该确权详情");
        }
        return toClaimDetailVO(claim);
    }

    @Override
    public ClaimDetailVO getClaimChainReceipt(String claimNo, Long viewerAccountId) {
        ClaimApplicationEntity claim = claimApplicationMapper.selectByClaimNo(claimNo);
        if (claim == null) {
            throw new BizException(ErrorCodeEnum.CLAIM_NOT_FOUND);
        }
        if (!claim.getApplicantAccountId().equals(viewerAccountId) && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该确权链回执");
        }
        return toClaimDetailVoRaw(claim);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewClaim(Long reviewerId, ClaimReviewRequest request) {
        log.info("审核确权申请，reviewerId={}, claimNo={}, result={}",
                reviewerId, request.getClaimNo(), request.getReviewResult());

        // 校验确权申请存在
        ClaimApplicationEntity claim = claimApplicationMapper.selectByClaimNo(request.getClaimNo());
        if (claim == null) {
            throw new BizException(ErrorCodeEnum.CLAIM_NOT_FOUND);
        }

        // 校验状态允许审核
        String claimStatus = claim.getStatus();
        if (!ClaimStatusEnum.CLAIM_SUBMITTED.getCode().equals(claimStatus)
                && !ClaimStatusEnum.CLAIM_REVIEWING.getCode().equals(claimStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前状态不允许审核", null, claimStatus);
        }

        // 加载关联作品
        WorkEntity work = workMapper.selectById(claim.getWorkId());
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        // 创建审核记录
        ClaimReviewRecordEntity reviewRecord = new ClaimReviewRecordEntity();
        reviewRecord.setClaimId(claim.getId());
        reviewRecord.setReviewerId(reviewerId);
        reviewRecord.setReviewAction("REVIEW");
        reviewRecord.setReviewResult(request.getReviewResult());
        reviewRecord.setReviewComment(request.getReviewComment());
        reviewRecord.setReasonCode(request.getReasonCode());
        reviewRecord.setReviewTime(now);
        claimReviewRecordMapper.insert(reviewRecord);

        ReviewResultEnum reviewResult = ReviewResultEnum.fromCode(request.getReviewResult());

        if (ReviewResultEnum.APPROVED.equals(reviewResult)) {
            handleClaimApproved(claim, work, reviewerId, request, now);
        } else if (ReviewResultEnum.REJECTED.equals(reviewResult)) {
            handleClaimRejected(claim, work, reviewerId, request, now);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ClaimDetailVO> listClaims(Long accountId, String status, PageQuery query) {
        LambdaQueryWrapper<ClaimApplicationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClaimApplicationEntity::getApplicantAccountId, accountId)
                .eq(status != null && !status.isBlank(), ClaimApplicationEntity::getStatus, status)
                .orderByDesc(ClaimApplicationEntity::getCreatedAt);

        Page<ClaimApplicationEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<ClaimApplicationEntity> result = claimApplicationMapper.selectPage(page, wrapper);

        List<ClaimDetailVO> records = result.getRecords().stream()
                .map(this::toClaimDetailVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ClaimDetailVO> listAllClaims(String status, PageQuery query) {
        LambdaQueryWrapper<ClaimApplicationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null && !status.isBlank(), ClaimApplicationEntity::getStatus, status)
                .orderByDesc(ClaimApplicationEntity::getCreatedAt);

        Page<ClaimApplicationEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<ClaimApplicationEntity> result = claimApplicationMapper.selectPage(page, wrapper);

        List<ClaimDetailVO> records = result.getRecords().stream()
                .map(this::toClaimDetailVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    // ==================== 私有方法 ====================

    /**
     * 处理确权审核通过
     * <p>
     * 设置确权申请为待上链，调用链码上链。
     * 上链成功后设置为确权成功，上链失败设置为上链失败。
     * </p>
     */
    private void handleClaimApproved(ClaimApplicationEntity claim, WorkEntity work,
                                     Long reviewerId, ClaimReviewRequest request,
                                     LocalDateTime now) {
        String oldClaimStatus = claim.getStatus();
        String oldWorkStatus = work.getStatus();

        // 设置确权为审核通过待上链
        claim.setStatus(ClaimStatusEnum.CLAIM_APPROVED_PENDING_CHAIN.getCode());
        claim.setReviewerId(reviewerId);
        claim.setReviewComment(request.getReviewComment());
        claim.setReviewTime(now);
        claim.setApproveTime(now);
        claim.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        claimApplicationMapper.updateById(claim);

        // 更新作品状态为上链中
        work.setStatus(WorkStatusEnum.CLAIM_CHAIN_PENDING.getCode());
        workMapper.updateById(work);

        auditService.writeStatusHistory(
                BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                oldClaimStatus, ClaimStatusEnum.CLAIM_APPROVED_PENDING_CHAIN.getCode(),
                "审核通过", null, reviewerId);

        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                oldWorkStatus, WorkStatusEnum.CLAIM_CHAIN_PENDING.getCode(),
                "确权审核通过，待上链", null, reviewerId);

        // 计算摘要哈希
        String summaryHash = HashUtil.sha256(
                claim.getClaimNo() + "|" + work.getWorkNo() + "|"
                        + work.getFileHash() + "|" + work.getMetaHash());
        claim.setSummaryHash(summaryHash);
        claim.setChainSubmitTime(now);

        // 获取创作者DID信息
        DidRecordEntity didRecord = didRecordMapper.selectById(claim.getApplicantDidId());
        String creatorDid = didRecord != null ? didRecord.getDidValue() : "";

        // 调用链码上链
        log.info("确权上链，claimNo={}, summaryHash={}", claim.getClaimNo(), summaryHash);
        ChainSubmitResult chainResult = claimChainAdapter.registerClaim(
                claim.getId(), claim.getClaimNo(), work.getWorkNo(),
                creatorDid, work.getFileHash(), work.getMetaHash(),
                summaryHash, now);

        if (chainResult.isSuccess()) {
            // 链提交成功 → CHAIN_SUBMITTED，等待回执确认后再推进到 CLAIM_SUCCESS
            claim.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
            claim.setTxHash(chainResult.getTxHash());
            claim.setBlockHeight(chainResult.getBlockHeight());
            claimApplicationMapper.updateById(claim);

            auditService.writeStatusHistory(
                    BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                    ClaimStatusEnum.CLAIM_APPROVED_PENDING_CHAIN.getCode(),
                    ClaimStatusEnum.CLAIM_APPROVED_PENDING_CHAIN.getCode(),
                    "确权链上交易已提交，等待回执确认", null, reviewerId);

            auditService.writeStatusHistory(
                    BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                    WorkStatusEnum.CLAIM_CHAIN_PENDING.getCode(),
                    WorkStatusEnum.CLAIM_CHAIN_PENDING.getCode(),
                    "确权链上交易已提交，等待回执", null, reviewerId);

            log.info("确权链上交易已提交待回执，claimNo={}, txHash={}", claim.getClaimNo(), chainResult.getTxHash());

            traceEventService.writeTraceEvent(BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                    "CLAIM_CHAIN_SUBMITTED", "确权审核通过，链上交易已提交", reviewerId, "REVIEWER", null);
        } else {
            // 上链失败
            claim.setStatus(ClaimStatusEnum.CLAIM_CHAIN_FAILED.getCode());
            claim.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            claim.setFailReason(chainResult.getFailReason());
            claimApplicationMapper.updateById(claim);

            work.setStatus(WorkStatusEnum.CLAIM_FAILED.getCode());
            workMapper.updateById(work);

            auditService.writeStatusHistory(
                    BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                    ClaimStatusEnum.CLAIM_APPROVED_PENDING_CHAIN.getCode(),
                    ClaimStatusEnum.CLAIM_CHAIN_FAILED.getCode(),
                    "链上确权失败：" + chainResult.getFailReason(), null, reviewerId);

            auditService.writeStatusHistory(
                    BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                    WorkStatusEnum.CLAIM_CHAIN_PENDING.getCode(),
                    WorkStatusEnum.CLAIM_FAILED.getCode(),
                    "确权失败", null, reviewerId);

            auditService.writeAuditLog(
                    BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                    "CHAIN_FAIL", "链上确权失败：" + chainResult.getFailReason(),
                    reviewerId, null, null, "FAIL", chainResult.getReasonCode());

            traceEventService.writeTraceEvent(BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                    "CLAIM_CHAIN_FAILED", "链上确权失败", reviewerId, "REVIEWER", null);

            log.error("确权上链失败，claimNo={}, reason={}", claim.getClaimNo(), chainResult.getFailReason());
        }
    }

    /**
     * 处理确权审核驳回
     * <p>
     * 设置确权申请为已驳回，作品状态回退到可确权。
     * </p>
     */
    private void handleClaimRejected(ClaimApplicationEntity claim, WorkEntity work,
                                     Long reviewerId, ClaimReviewRequest request,
                                     LocalDateTime now) {
        String oldClaimStatus = claim.getStatus();
        String oldWorkStatus = work.getStatus();

        // 设置确权为已驳回
        claim.setStatus(ClaimStatusEnum.CLAIM_REJECTED.getCode());
        claim.setReviewerId(reviewerId);
        claim.setReviewComment(request.getReviewComment());
        claim.setReviewTime(now);
        claim.setRejectReason(request.getReviewComment());
        claim.setReasonCode(request.getReasonCode());
        claimApplicationMapper.updateById(claim);

        // 作品回退到可确权
        work.setStatus(WorkStatusEnum.READY_FOR_CLAIM.getCode());
        workMapper.updateById(work);

        auditService.writeStatusHistory(
                BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                oldClaimStatus, ClaimStatusEnum.CLAIM_REJECTED.getCode(),
                "审核驳回：" + request.getReviewComment(),
                request.getReasonCode(), reviewerId);

        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                oldWorkStatus, WorkStatusEnum.READY_FOR_CLAIM.getCode(),
                "确权申请被驳回", request.getReasonCode(), reviewerId);

        auditService.writeAuditLog(
                BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                "REJECT", "审核驳回：" + request.getReviewComment(),
                reviewerId, null, null, "SUCCESS", request.getReasonCode());

        traceEventService.writeTraceEvent(BizTypeEnum.CLAIM.getCode(), claim.getId(), claim.getClaimNo(),
                "CLAIM_REJECTED", "确权申请被驳回", reviewerId, "REVIEWER", null);

        log.info("确权审核驳回，claimNo={}", claim.getClaimNo());
    }

    /**
     * 转换为确权详情视图对象
     */
    private ClaimDetailVO toClaimDetailVO(ClaimApplicationEntity entity) {
        ClaimDetailVO vo = new ClaimDetailVO();

        var basic = new ClaimDetailVO.BasicInfo();
        basic.setClaimNo(entity.getClaimNo());
        basic.setSummaryHash(entity.getSummaryHash());
        vo.setBasicInfo(basic);

        var status = new ClaimDetailVO.StatusInfo();
        status.setStatus(entity.getStatus());
        status.setReviewComment(entity.getReviewComment());
        status.setRejectReason(entity.getRejectReason());
        vo.setStatusInfo(status);

        var time = new ClaimDetailVO.TimeInfo();
        time.setSubmitTime(entity.getSubmitTime());
        time.setReviewTime(entity.getReviewTime());
        time.setApproveTime(entity.getApproveTime());
        vo.setTimeInfo(time);

        var relation = new ClaimDetailVO.RelationInfo();
        relation.setWorkNo(entity.getWorkNo());
        vo.setRelationInfo(relation);

        var chain = new ClaimDetailVO.ChainInfo();
        chain.setChainStatus(entity.getChainStatus());
        chain.setTxHash(entity.getTxHash());
        chain.setBlockHeight(entity.getBlockHeight());
        vo.setChainInfo(chain);

        vo.setAllowedActions(computeClaimAllowedActions(entity));

        // 统一可见性装配
        WorkVoAssembler.applyVisibility(vo);

        return vo;
    }

    /**
     * 转换为确权详情视图对象（保留chainInfo，供链回执接口使用）
     */
    private ClaimDetailVO toClaimDetailVoRaw(ClaimApplicationEntity entity) {
        ClaimDetailVO vo = new ClaimDetailVO();

        var basic = new ClaimDetailVO.BasicInfo();
        basic.setClaimNo(entity.getClaimNo());
        basic.setSummaryHash(entity.getSummaryHash());
        vo.setBasicInfo(basic);

        var status = new ClaimDetailVO.StatusInfo();
        status.setStatus(entity.getStatus());
        vo.setStatusInfo(status);

        var time = new ClaimDetailVO.TimeInfo();
        time.setSubmitTime(entity.getSubmitTime());
        time.setApproveTime(entity.getApproveTime());
        vo.setTimeInfo(time);

        var relation = new ClaimDetailVO.RelationInfo();
        relation.setWorkNo(entity.getWorkNo());
        vo.setRelationInfo(relation);

        var chain = new ClaimDetailVO.ChainInfo();
        chain.setChainStatus(entity.getChainStatus());
        chain.setTxHash(entity.getTxHash());
        chain.setBlockHeight(entity.getBlockHeight());
        vo.setChainInfo(chain);

        return vo;
    }

    /**
     * 根据确权状态计算允许的操作列表
     */
    private List<String> computeClaimAllowedActions(ClaimApplicationEntity claim) {
        List<String> actions = new ArrayList<>();
        String status = claim.getStatus();

        if (ClaimStatusEnum.CLAIM_SUBMITTED.getCode().equals(status)
                || ClaimStatusEnum.CLAIM_REVIEWING.getCode().equals(status)) {
            actions.add("REVIEW");
        }
        if (ClaimStatusEnum.CLAIM_CHAIN_FAILED.getCode().equals(status)) {
            actions.add("RETRY_CHAIN");
        }

        return actions;
    }

}
