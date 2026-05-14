package com.lifechain.regulator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.chain.adapter.RegulatoryChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.*;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.regulator.dto.FreezeRecordVO;
import com.lifechain.regulator.dto.FreezeRequest;
import com.lifechain.regulator.dto.UnfreezeRequest;
import com.lifechain.regulator.entity.FreezeRecordEntity;
import com.lifechain.regulator.mapper.FreezeRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 冻结服务实现
 * <p>
 * 完整的冻结/解冻生命周期管理实现：
 * <ol>
 *   <li>冻结申请：校验目标未被冻结，生成冻结编号，创建冻结记录</li>
 *   <li>监管直接冻结：直接生效并提交链上存证</li>
 *   <li>审核冻结：需后续审批流程</li>
 *   <li>解冻操作：校验当前冻结状态，更新为解冻并记录原因</li>
 * </ol>
 * 冻结/解冻操作同步上链存证，所有状态变更记录审计日志和状态变更历史。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FreezeServiceImpl implements FreezeService {

    private final FreezeRecordMapper freezeRecordMapper;
    private final RegulatoryChainAdapter regulatoryChainAdapter;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final FreezeTargetService freezeTargetService;

    /**
     * {@inheritDoc}
     * <p>
     * 完整冻结流程：
     * <ol>
     *   <li>校验目标类型合法性</li>
     *   <li>校验目标未被活跃冻结</li>
     *   <li>生成冻结编号，创建冻结记录</li>
     *   <li>监管直接冻结：直接设为FREEZE_APPROVED，提交链上存证</li>
     *   <li>审核冻结：设为FREEZE_APPLIED，等待后续审批</li>
     *   <li>记录审计日志和状态变更历史</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreezeRecordVO freeze(Long operatorId, FreezeRequest request) {
        log.info("发起冻结，operatorId={}, targetType={}, targetNo={}, freezeMode={}",
                operatorId, request.getTargetType(), request.getTargetNo(), request.getFreezeMode());

        TargetTypeEnum.fromCode(request.getTargetType());

        boolean isDirect = "REGULATOR_DIRECT".equals(request.getFreezeMode());

        // 监管直接冻结必须提供紧急依据编号
        if (isDirect && (request.getUrgentBasisNo() == null || request.getUrgentBasisNo().isBlank())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "紧急直接冻结必须提供紧急依据编号(urgentBasisNo)");
        }

        // 通过 targetNo 解析 targetId
        Long targetId = null;
        if (freezeTargetService != null) {
            targetId = freezeTargetService.resolveTargetId(request.getTargetType(), request.getTargetNo());
        }
        if (targetId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "目标业务对象不存在: " + request.getTargetNo());
        }
        String previousTargetStatus = freezeTargetService.captureCurrentStatus(request.getTargetType(), request.getTargetNo());
        // 校验目标未被活跃冻结
        List<FreezeRecordEntity> activeList = freezeRecordMapper
                .selectActiveFreezeByTargetNo(request.getTargetType(), request.getTargetNo());
        if (!activeList.isEmpty()) {
            throw new BizException(ErrorCodeEnum.FREEZE_APPLY_FAILED,
                    "目标已存在生效中的冻结记录: " + activeList.get(0).getFreezeNo());
        }

        // 获取申请人角色
        String applyRole = "REGULATOR";
        UserContext.UserInfo userInfo = UserContext.get();
        if (userInfo != null && userInfo.getRoles() != null && !userInfo.getRoles().isEmpty()) {
            applyRole = userInfo.getRoles().get(0);
        }

        LocalDateTime now = DateTimeUtil.nowUtc();
        String freezeNo = BizNoUtil.freezeNo();

        FreezeRecordEntity entity = new FreezeRecordEntity();
        entity.setFreezeNo(freezeNo);
        entity.setTargetType(request.getTargetType());
        entity.setTargetId(targetId);
        entity.setTargetNo(request.getTargetNo());
        entity.setPreviousTargetStatus(previousTargetStatus);
        entity.setFreezeMode(request.getFreezeMode());
        entity.setFreezeReason(request.getFreezeReason());
        entity.setReasonCode(request.getReasonCode());
        entity.setApplyUserId(operatorId);
        entity.setApplyRole(applyRole);
        entity.setApplyTime(now);
        entity.setUrgentBasisNo(request.getUrgentBasisNo());
        entity.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        String initialStatus;
        if (isDirect) {
            // 紧急直接冻结：进入待上链确认状态，回执成功后才真正生效
            initialStatus = FreezeStatusEnum.FREEZE_APPROVED_PENDING_CHAIN.getCode();
            entity.setFreezeStatus(initialStatus);
            entity.setApproveUserId(operatorId);
            entity.setApproveTime(now);
            entity.setReviewStatus("PENDING_POST_REVIEW");
        } else {
            // 审核冻结：需平台复核后生效
            initialStatus = FreezeStatusEnum.FREEZE_APPLIED.getCode();
            entity.setFreezeStatus(initialStatus);
            entity.setReviewStatus("PENDING_REVIEW");
        }

        freezeRecordMapper.insert(entity);
        log.info("冻结记录创建成功，freezeNo={}, status={}, reviewStatus={}", freezeNo, initialStatus, entity.getReviewStatus());

        // 监管直接冻结：提交链上存证（不再立即联动冻结目标对象，等回执确认后再执行）
        if (isDirect) {
            submitFreezeToChain(entity, now);
        }

        // 审计日志
        auditService.writeAuditLog(
                request.getTargetType(), targetId, request.getTargetNo(),
                "FREEZE_APPLY", "发起冻结: " + freezeNo + ", 模式: " + request.getFreezeMode()
                        + (isDirect ? ", 紧急依据: " + request.getUrgentBasisNo() : ""),
                operatorId, applyRole, null, "SUCCESS", null);

        auditService.writeStatusHistory(
                BizTypeEnum.FREEZE.getCode(), entity.getId(), freezeNo,
                null, initialStatus,
                "发起冻结: " + request.getFreezeReason(), null, operatorId);

        traceEventService.writeTraceEvent(BizTypeEnum.FREEZE.getCode(), entity.getId(), freezeNo,
                "FREEZE_APPLIED", "发起冻结", operatorId, applyRole, null);

        return toVO(entity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 解冻流程：
     * <ol>
     *   <li>校验冻结记录存在</li>
     *   <li>校验状态为FREEZE_APPROVED（已生效冻结）</li>
     *   <li>更新状态为UNFREEZE_APPROVED</li>
     *   <li>记录解冻原因和时间</li>
     *   <li>提交解冻上链存证</li>
     *   <li>记录状态变更历史</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreezeRecordVO unfreeze(Long operatorId, UnfreezeRequest request) {
        log.info("发起解冻，operatorId={}, freezeNo={}", operatorId, request.getFreezeNo());

        FreezeRecordEntity entity = freezeRecordMapper.selectByFreezeNo(request.getFreezeNo());
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.UNFREEZE_FAILED, "冻结记录不存在: " + request.getFreezeNo());
        }
        if (!FreezeStatusEnum.FREEZE_APPROVED.getCode().equals(entity.getFreezeStatus())
                && !FreezeStatusEnum.FREEZE_APPROVED_PENDING_CHAIN.getCode().equals(entity.getFreezeStatus())) {
            throw new BizException(ErrorCodeEnum.UNFREEZE_FAILED,
                    "当前冻结状态不允许解冻", null, entity.getFreezeStatus());
        }

        String fromStatus = entity.getFreezeStatus();
        LocalDateTime now = DateTimeUtil.nowUtc();
        String toStatus = FreezeStatusEnum.UNFREEZE_PENDING_CHAIN.getCode();

        entity.setFreezeStatus(toStatus);
        entity.setUnfreezeReason(request.getUnfreezeReason());
        entity.setUpdatedAt(now);
        freezeRecordMapper.updateById(entity);

        log.info("解冻申请已提交，freezeNo={}, fromStatus={}, toStatus={}", request.getFreezeNo(), fromStatus, toStatus);

        // 解冻上链
        try {
            ChainSubmitResult chainResult = regulatoryChainAdapter.registerUnfreeze(
                    entity.getId(), entity.getFreezeNo(),
                    request.getUnfreezeReason(), now);
            if (chainResult.isSuccess()) {
                entity.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                entity.setTxHash(chainResult.getTxHash());
                entity.setBlockHeight(chainResult.getBlockHeight());
            } else {
                entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                log.warn("解冻上链失败，freezeNo={}, reason={}", entity.getFreezeNo(), chainResult.getFailReason());
            }
            entity.setUpdatedAt(DateTimeUtil.nowUtc());
            freezeRecordMapper.updateById(entity);
        } catch (Exception e) {
            entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            entity.setUpdatedAt(DateTimeUtil.nowUtc());
            freezeRecordMapper.updateById(entity);
            log.error("解冻上链异常，freezeNo={}", entity.getFreezeNo(), e);
        }

        auditService.writeStatusHistory(
                BizTypeEnum.FREEZE.getCode(), entity.getId(), entity.getFreezeNo(),
                fromStatus, toStatus,
                "解冻: " + request.getUnfreezeReason(), null, operatorId);

        // 解冻通知与“解冻完成”轨迹改为回执成功后再发，避免链上失败时提前宣告完成

        return toVO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreezeRecordVO getFreezeRecord(String freezeNo) {
        FreezeRecordEntity entity = freezeRecordMapper.selectByFreezeNo(freezeNo);
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.FREEZE_APPLY_FAILED, "冻结记录不存在: " + freezeNo);
        }
        return toVO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<FreezeRecordVO> listFreezeRecords(String targetType, String status, PageQuery query) {
        Page<FreezeRecordEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<FreezeRecordEntity> wrapper = new LambdaQueryWrapper<FreezeRecordEntity>()
                .eq(targetType != null, FreezeRecordEntity::getTargetType, targetType)
                .eq(status != null, FreezeRecordEntity::getFreezeStatus, status)
                .orderByDesc(FreezeRecordEntity::getCreatedAt);
        Page<FreezeRecordEntity> result = freezeRecordMapper.selectPage(page, wrapper);

        List<FreezeRecordVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FreezeRecordVO reviewFreeze(Long operatorId, String freezeNo, boolean approved, String reviewNote) {
        log.info("事后复核冻结，operatorId={}, freezeNo={}, approved={}", operatorId, freezeNo, approved);

        FreezeRecordEntity entity = freezeRecordMapper.selectByFreezeNo(freezeNo);
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.FREEZE_APPLY_FAILED, "冻结记录不存在: " + freezeNo);
        }
        if (!"PENDING_POST_REVIEW".equals(entity.getReviewStatus())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "当前复核状态不允许复核操作，当前: " + entity.getReviewStatus());
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        if (approved) {
            entity.setReviewStatus("REVIEW_PASSED");
            entity.setUpdatedAt(now);
            freezeRecordMapper.updateById(entity);
            log.info("冻结事后复核通过，freezeNo={}", freezeNo);

            auditService.writeAuditLog(
                    "FREEZE", entity.getId(), freezeNo,
                    "FREEZE_REVIEW_PASS", "事后复核通过" + (reviewNote != null ? ": " + reviewNote : ""),
                    operatorId, "PLATFORM_ADMIN", null, "SUCCESS", null);

            traceEventService.writeTraceEvent(BizTypeEnum.FREEZE.getCode(), entity.getId(), freezeNo,
                    "FREEZE_REVIEW_PASSED", "事后复核通过", operatorId, "PLATFORM_ADMIN", null);
        } else {
            // 复核不通过，自动解冻
            entity.setReviewStatus("REVIEW_REJECTED");
            String fromStatus = entity.getFreezeStatus();

            if (FreezeStatusEnum.FREEZE_APPROVED.getCode().equals(fromStatus)) {
                // 冻结已链上生效，需走链上解冻流程
                entity.setFreezeStatus(FreezeStatusEnum.UNFREEZE_PENDING_CHAIN.getCode());
                entity.setUnfreezeReason("事后复核未通过自动解冻" + (reviewNote != null ? ": " + reviewNote : ""));
                entity.setUpdatedAt(now);
                freezeRecordMapper.updateById(entity);

                // 提交解冻上链
                try {
                    ChainSubmitResult chainResult = regulatoryChainAdapter.registerUnfreeze(
                            entity.getId(), entity.getFreezeNo(),
                            entity.getUnfreezeReason(), now);
                    if (chainResult.isSuccess()) {
                        entity.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                        entity.setTxHash(chainResult.getTxHash());
                        entity.setBlockHeight(chainResult.getBlockHeight());
                    } else {
                        entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                    }
                    entity.setUpdatedAt(DateTimeUtil.nowUtc());
                    freezeRecordMapper.updateById(entity);
                } catch (Exception e) {
                    entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                    entity.setUpdatedAt(DateTimeUtil.nowUtc());
                    freezeRecordMapper.updateById(entity);
                    log.error("事后复核驳回解冻上链异常，freezeNo={}", freezeNo, e);
                }
            } else {
                // 冻结交易已提交但尚未确认：标记为“复核驳回，待原冻结回执后自动解冻”
                entity.setUnfreezeReason("事后复核未通过自动解冻" + (reviewNote != null ? ": " + reviewNote : ""));
                entity.setUpdatedAt(now);
                entity.setReviewStatus("REVIEW_REJECTED_PENDING_UNFREEZE");
                freezeRecordMapper.updateById(entity);
            }

            auditService.writeAuditLog(
                    "FREEZE", entity.getId(), freezeNo,
                    "FREEZE_REVIEW_REJECT", "事后复核驳回并自动解冻" + (reviewNote != null ? ": " + reviewNote : ""),
                    operatorId, "PLATFORM_ADMIN", null, "SUCCESS", null);

            auditService.writeStatusHistory(
                    BizTypeEnum.FREEZE.getCode(), entity.getId(), freezeNo,
                    fromStatus, entity.getFreezeStatus(),
                    FreezeStatusEnum.FREEZE_APPROVED.getCode().equals(fromStatus)
                            ? "事后复核驳回，已提交自动解冻"
                            : "事后复核驳回，等待原冻结回执后自动解冻",
                    null, operatorId);

            traceEventService.writeTraceEvent(BizTypeEnum.FREEZE.getCode(), entity.getId(), freezeNo,
                    "FREEZE_REVIEW_REJECTED",
                    FreezeStatusEnum.FREEZE_APPROVED.getCode().equals(fromStatus)
                            ? "事后复核驳回，自动解冻处理中"
                            : "事后复核驳回，等待原冻结回执后自动解冻",
                    operatorId, "PLATFORM_ADMIN", null);
        }

        return toVO(entity);
    }

    /**
     * 提交冻结上链存证
     *
     * @param entity    冻结记录实体
     * @param freezeTime 冻结时间
     */
    private void submitFreezeToChain(FreezeRecordEntity entity, LocalDateTime freezeTime) {
        try {
            ChainSubmitResult chainResult = regulatoryChainAdapter.registerFreeze(
                    entity.getId(), entity.getFreezeNo(),
                    entity.getTargetType(), entity.getTargetNo(),
                    entity.getFreezeReason(), freezeTime);
            if (chainResult.isSuccess()) {
                entity.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                entity.setTxHash(chainResult.getTxHash());
                entity.setBlockHeight(chainResult.getBlockHeight());
                log.info("冻结链上交易已提交待回执，freezeNo={}, txHash={}", entity.getFreezeNo(), chainResult.getTxHash());
            } else {
                entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                log.warn("冻结上链失败，freezeNo={}, reason={}", entity.getFreezeNo(), chainResult.getFailReason());
            }
            entity.setUpdatedAt(DateTimeUtil.nowUtc());
            freezeRecordMapper.updateById(entity);
        } catch (Exception e) {
            entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            entity.setUpdatedAt(DateTimeUtil.nowUtc());
            freezeRecordMapper.updateById(entity);
            log.error("冻结上链异常，freezeNo={}", entity.getFreezeNo(), e);
        }
    }

    /**
     * 实体转视图对象
     *
     * @param entity 冻结记录实体
     * @return 冻结记录视图对象
     */
    private FreezeRecordVO toVO(FreezeRecordEntity entity) {
        FreezeRecordVO vo = new FreezeRecordVO();
        vo.setFreezeNo(entity.getFreezeNo());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetNo(entity.getTargetNo());
        vo.setFreezeStatus(entity.getFreezeStatus());
        vo.setFreezeMode(entity.getFreezeMode());
        vo.setReviewStatus(entity.getReviewStatus());
        vo.setApplyRole(entity.getApplyRole());
        vo.setReasonCode(entity.getReasonCode());
        vo.setFreezeReason(entity.getFreezeReason());
        vo.setApplyTime(entity.getApplyTime());
        vo.setApproveTime(entity.getApproveTime());
        vo.setEffectiveTime(entity.getEffectiveTime());
        vo.setUnfreezeTime(entity.getUnfreezeTime());
        vo.setUnfreezeReason(entity.getUnfreezeReason());
        vo.setUrgentBasisNo(entity.getUrgentBasisNo());
        vo.setChainStatus(entity.getChainStatus());
        vo.setTxHash(entity.getTxHash());
        vo.setBlockHeight(entity.getBlockHeight());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}

