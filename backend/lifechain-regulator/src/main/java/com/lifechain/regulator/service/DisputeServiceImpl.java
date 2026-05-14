package com.lifechain.regulator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.chain.adapter.RegulatoryChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.DisputeStatusEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.infra.attachment.SysAttachmentEntity;
import com.lifechain.infra.attachment.SysAttachmentMapper;
import com.lifechain.infra.storage.StorageService;
import com.lifechain.regulator.assembler.DisputeVoAssembler;
import com.lifechain.regulator.dto.*;
import com.lifechain.regulator.entity.DisputeCaseEntity;
import com.lifechain.regulator.entity.DisputeEvidenceEntity;
import com.lifechain.regulator.entity.DisputeProcessRecordEntity;
import com.lifechain.regulator.mapper.DisputeCaseMapper;
import com.lifechain.regulator.mapper.DisputeEvidenceMapper;
import com.lifechain.regulator.mapper.DisputeProcessRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 争议案件服务实现
 * <p>
 * 完整的争议案件生命周期管理实现，涵盖案件创建、证据补充、处理流转和查询：
 * <ol>
 *   <li>创建案件：生成编号，初始状态为DISPUTE_SUBMITTED，创建初始处理记录</li>
 *   <li>补充证据：校验案件未关闭，创建证据记录（含文件哈希），添加处理记录</li>
 *   <li>处理流转：完整状态机，支持受理、要求补证、审查、解决、驳回、关闭</li>
 *   <li>结案上链：解决/驳回/关闭时将结论上链存证</li>
 * </ol>
 * 每步操作均记录处理记录、审计日志和状态变更历史，确保全链路可追溯。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeServiceImpl implements DisputeService {

    private final DisputeCaseMapper disputeCaseMapper;
    private final DisputeEvidenceMapper disputeEvidenceMapper;
    private final DisputeProcessRecordMapper disputeProcessRecordMapper;
    private final DisputeBusinessValidator disputeBusinessValidator;
    private final RegulatoryChainAdapter regulatoryChainAdapter;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final AccountMapper accountMapper;
    private final SysAttachmentMapper attachmentMapper;
    private final StorageService storageService;

    /** 已关闭的终态集合（含待链上确认的中间态） */
    private static final Set<String> CLOSED_STATUSES = Set.of(
            DisputeStatusEnum.DISPUTE_RESOLVED.getCode(),
            DisputeStatusEnum.DISPUTE_RESOLVED_PENDING_CHAIN.getCode(),
            DisputeStatusEnum.DISPUTE_REJECTED.getCode(),
            DisputeStatusEnum.DISPUTE_REJECTED_PENDING_CHAIN.getCode(),
            DisputeStatusEnum.DISPUTE_CLOSED.getCode(),
            DisputeStatusEnum.DISPUTE_CLOSED_PENDING_CHAIN.getCode()
    );

    /**
     * {@inheritDoc}
     * <p>
     * 完整案件创建流程：
     * <ol>
     *   <li>生成案件编号</li>
     *   <li>创建争议案件实体（状态=DISPUTE_SUBMITTED）</li>
     *   <li>若提供初始证据URL列表，批量创建证据记录</li>
     *   <li>创建初始处理记录"案件创建"</li>
     *   <li>记录审计日志和状态变更历史</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeCaseVO createDispute(Long applicantAccountId, CreateDisputeRequest request) {
        log.info("创建争议案件，applicantAccountId={}, disputeType={}, orderNo={}",
                applicantAccountId, request.getDisputeType(), request.getOrderNo());

        // 解析被申请人accountNo → accountId
        AccountEntity respondentAccount = accountMapper.selectByAccountNo(request.getRespondentAccountNo());
        if (respondentAccount == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "被申请人账户不存在: " + request.getRespondentAccountNo());
        }
        Long respondentAccountId = respondentAccount.getId();

        // 业务关联校验：至少需要关联订单或作品
        if ((request.getOrderNo() == null || request.getOrderNo().isBlank())
                && (request.getWorkNo() == null || request.getWorkNo().isBlank())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "争议必须关联订单或作品");
        }

        // 校验订单存在性和当事人关系
        if (request.getOrderNo() != null && !request.getOrderNo().isBlank()) {
            if (!disputeBusinessValidator.orderExists(request.getOrderNo())) {
                throw new BizException(ErrorCodeEnum.PARAM_INVALID, "关联订单不存在: " + request.getOrderNo());
            }
            if (!disputeBusinessValidator.isOrderParty(request.getOrderNo(), applicantAccountId)) {
                throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "申请人不是该订单的相关方");
            }
            if (!disputeBusinessValidator.isOrderCounterparty(request.getOrderNo(), applicantAccountId, respondentAccountId)) {
                throw new BizException(ErrorCodeEnum.PARAM_INVALID, "被申请人不是该订单的对手方");
            }
        }

        // 校验作品存在性和创作者关系
        if (request.getWorkNo() != null && !request.getWorkNo().isBlank()) {
            if (!disputeBusinessValidator.workExists(request.getWorkNo())) {
                throw new BizException(ErrorCodeEnum.PARAM_INVALID, "关联作品不存在: " + request.getWorkNo());
            }
            if ((request.getOrderNo() == null || request.getOrderNo().isBlank())
                    && !disputeBusinessValidator.isWorkDisputeParty(
                    request.getWorkNo(), applicantAccountId, respondentAccountId)) {
                throw new BizException(ErrorCodeEnum.PARAM_INVALID, "作品争议必须由作品创作者及其相对方发起");
            }
        }

        // 校验同一业务对象是否已有未关闭争议
        LambdaQueryWrapper<DisputeCaseEntity> duplicateWrapper = new LambdaQueryWrapper<DisputeCaseEntity>()
                .eq(request.getOrderNo() != null, DisputeCaseEntity::getOrderNo, request.getOrderNo())
                .eq(request.getWorkNo() != null, DisputeCaseEntity::getWorkNo, request.getWorkNo())
                .notIn(DisputeCaseEntity::getStatus,
                        DisputeStatusEnum.DISPUTE_RESOLVED.getCode(),
                        DisputeStatusEnum.DISPUTE_REJECTED.getCode(),
                        DisputeStatusEnum.DISPUTE_CLOSED.getCode());
        if (disputeCaseMapper.selectCount(duplicateWrapper) > 0) {
            throw new BizException(ErrorCodeEnum.DISPUTE_SUBMIT_FAILED, "该业务对象已有未关闭的争议案件");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();
        String caseNo = BizNoUtil.caseNo();

        // 创建案件实体
        DisputeCaseEntity caseEntity = new DisputeCaseEntity();
        caseEntity.setCaseNo(caseNo);
        caseEntity.setOrderNo(request.getOrderNo());
        caseEntity.setWorkNo(request.getWorkNo());
        caseEntity.setApplicantAccountId(applicantAccountId);
        caseEntity.setRespondentAccountId(respondentAccountId);
        caseEntity.setDisputeType(request.getDisputeType());
        caseEntity.setStatus(DisputeStatusEnum.DISPUTE_SUBMITTED.getCode());
        caseEntity.setDescription(request.getDescription());
        caseEntity.setSubmitTime(now);
        caseEntity.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        caseEntity.setCreatedAt(now);
        caseEntity.setUpdatedAt(now);
        disputeCaseMapper.insert(caseEntity);

        log.info("争议案件创建成功，caseNo={}", caseNo);

        // 批量创建初始证据
        if (request.getEvidenceUrls() != null && !request.getEvidenceUrls().isEmpty()) {
            for (String evidenceUrl : request.getEvidenceUrls()) {
                createEvidenceRecord(caseEntity.getId(), caseNo, applicantAccountId,
                        "INITIAL", evidenceUrl, "案件创建时提交的初始证据", null, now);
            }
        }

        // 创建初始处理记录
        createProcessRecord(caseEntity.getId(), caseNo, applicantAccountId,
                "CREATE", "SUCCESS", "案件创建", null, now);

        // 审计日志
        auditService.writeAuditLog(
                BizTypeEnum.DISPUTE.getCode(), caseEntity.getId(), caseNo,
                "DISPUTE_CREATE", "创建争议案件, 类型: " + request.getDisputeType(),
                applicantAccountId, null, null, "SUCCESS", null);

        auditService.writeStatusHistory(
                BizTypeEnum.DISPUTE.getCode(), caseEntity.getId(), caseNo,
                null, DisputeStatusEnum.DISPUTE_SUBMITTED.getCode(),
                "创建争议案件", null, applicantAccountId);

        traceEventService.writeTraceEvent(BizTypeEnum.DISPUTE.getCode(), caseEntity.getId(), caseNo,
                "DISPUTE_CREATED", "争议案件已创建", applicantAccountId, null, null);

        return getDisputeDetailInternal(caseNo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeCaseVO addEvidence(Long submitterAccountId, String caseNo,
                                     String evidenceType, String fileUrl, String description, String fileHash) {
        log.info("补充证据，submitterAccountId={}, caseNo={}, evidenceType={}",
                submitterAccountId, caseNo, evidenceType);

        DisputeCaseEntity caseEntity = disputeCaseMapper.selectByCaseNo(caseNo);
        if (caseEntity == null) {
            throw new BizException(ErrorCodeEnum.DISPUTE_SUBMIT_FAILED, "争议案件不存在: " + caseNo);
        }
        if (CLOSED_STATUSES.contains(caseEntity.getStatus())) {
            throw new BizException(ErrorCodeEnum.DISPUTE_ALREADY_CLOSED,
                    "争议案件已关闭，不可补充证据", null, caseEntity.getStatus());
        }

        // 校验提交人是否为案件相关方或管理角色
        boolean isApplicant = submitterAccountId.equals(caseEntity.getApplicantAccountId());
        boolean isRespondent = caseEntity.getRespondentAccountId() != null
                && submitterAccountId.equals(caseEntity.getRespondentAccountId());
        if (!isApplicant && !isRespondent && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "仅案件相关方或管理员可补充证据");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();
        createEvidenceRecord(caseEntity.getId(), caseNo, submitterAccountId,
                evidenceType, fileUrl, description, fileHash, now);

        // 添加处理记录
        createProcessRecord(caseEntity.getId(), caseNo, submitterAccountId,
                "SUBMIT_EVIDENCE", null, "提交证据: " + evidenceType, null, now);

        log.info("证据补充成功，caseNo={}", caseNo);

        traceEventService.writeTraceEvent(BizTypeEnum.DISPUTE.getCode(), caseEntity.getId(), caseNo,
                "DISPUTE_EVIDENCE_ADDED", "补充证据: " + evidenceType, submitterAccountId, null, null);

        return getDisputeDetailInternal(caseNo);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 完整状态机流转实现：
     * <ul>
     *   <li>ACCEPT: DISPUTE_SUBMITTED → DISPUTE_ACCEPTED（设置受理时间）</li>
     *   <li>EVIDENCE_PENDING: DISPUTE_ACCEPTED → DISPUTE_EVIDENCE_PENDING</li>
     *   <li>REVIEW: DISPUTE_ACCEPTED/DISPUTE_EVIDENCE_PENDING → DISPUTE_REVIEWING</li>
     *   <li>RESOLVE: 任意非关闭状态 → DISPUTE_RESOLVED（设置结案时间，结论上链）</li>
     *   <li>REJECT: 任意非关闭状态 → DISPUTE_REJECTED（设置结案时间，结论上链）</li>
     *   <li>CLOSE: 任意非关闭状态 → DISPUTE_CLOSED（设置结案时间，结论上链）</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DisputeCaseVO processDispute(Long operatorId, DisputeProcessRequest request) {
        log.info("处理争议案件，operatorId={}, caseNo={}, action={}",
                operatorId, request.getCaseNo(), request.getAction());

        DisputeCaseEntity caseEntity = disputeCaseMapper.selectByCaseNo(request.getCaseNo());
        if (caseEntity == null) {
            throw new BizException(ErrorCodeEnum.DISPUTE_PROCESS_FAILED,
                    "争议案件不存在: " + request.getCaseNo());
        }
        if (CLOSED_STATUSES.contains(caseEntity.getStatus())) {
            throw new BizException(ErrorCodeEnum.DISPUTE_ALREADY_CLOSED,
                    "争议案件已关闭", null, caseEntity.getStatus());
        }

        String fromStatus = caseEntity.getStatus();
        LocalDateTime now = DateTimeUtil.nowUtc();
        String toStatus;
        boolean isClosing = false;

        switch (request.getAction().toUpperCase()) {
            case "ACCEPT" -> {
                validateTransition(fromStatus, DisputeStatusEnum.DISPUTE_SUBMITTED.getCode(), "受理");
                toStatus = DisputeStatusEnum.DISPUTE_ACCEPTED.getCode();
                caseEntity.setAcceptTime(now);
            }
            case "EVIDENCE_PENDING" -> {
                validateTransition(fromStatus, DisputeStatusEnum.DISPUTE_ACCEPTED.getCode(), "要求补充证据");
                toStatus = DisputeStatusEnum.DISPUTE_EVIDENCE_PENDING.getCode();
            }
            case "REVIEW" -> {
                if (!DisputeStatusEnum.DISPUTE_ACCEPTED.getCode().equals(fromStatus)
                        && !DisputeStatusEnum.DISPUTE_EVIDENCE_PENDING.getCode().equals(fromStatus)) {
                    throw new BizException(ErrorCodeEnum.STATUS_TRANSITION_DENIED,
                            "当前状态不允许进入审查", null, fromStatus);
                }
                toStatus = DisputeStatusEnum.DISPUTE_REVIEWING.getCode();
            }
            case "RESOLVE" -> {
                toStatus = DisputeStatusEnum.DISPUTE_RESOLVED_PENDING_CHAIN.getCode();
                caseEntity.setCloseTime(now);
                caseEntity.setResultSummary(request.getResultSummary());
                caseEntity.setReasonCode(request.getReasonCode());
                isClosing = true;
            }
            case "REJECT" -> {
                toStatus = DisputeStatusEnum.DISPUTE_REJECTED_PENDING_CHAIN.getCode();
                caseEntity.setCloseTime(now);
                caseEntity.setResultSummary(request.getResultSummary());
                caseEntity.setReasonCode(request.getReasonCode());
                isClosing = true;
            }
            case "CLOSE" -> {
                toStatus = DisputeStatusEnum.DISPUTE_CLOSED_PENDING_CHAIN.getCode();
                caseEntity.setCloseTime(now);
                caseEntity.setResultSummary(request.getResultSummary());
                caseEntity.setReasonCode(request.getReasonCode());
                isClosing = true;
            }
            default -> throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "不支持的处理动作: " + request.getAction());
        }

        caseEntity.setStatus(toStatus);
        caseEntity.setUpdatedAt(now);
        disputeCaseMapper.updateById(caseEntity);

        log.info("争议案件处理完成，caseNo={}, fromStatus={}, toStatus={}",
                request.getCaseNo(), fromStatus, toStatus);

        // 创建处理记录
        createProcessRecord(caseEntity.getId(), caseEntity.getCaseNo(), operatorId,
                request.getAction().toUpperCase(), toStatus, request.getComment(),
                request.getReasonCode(), now);

        // 结案时将结论上链（联动动作移至 DisputeChainReceiptHandler 回执成功后执行）
        if (isClosing) {
            submitDisputeConclusionToChain(caseEntity, toStatus, now);
        }

        // 状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.DISPUTE.getCode(), caseEntity.getId(), caseEntity.getCaseNo(),
                fromStatus, toStatus,
                "处理争议: " + request.getAction(), request.getReasonCode(), operatorId);

        traceEventService.writeTraceEvent(BizTypeEnum.DISPUTE.getCode(), caseEntity.getId(), caseEntity.getCaseNo(),
                "DISPUTE_PROCESSED", "争议处理: " + request.getAction(), operatorId, null, null);

        return getDisputeDetailInternal(request.getCaseNo());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DisputeCaseVO getDisputeDetail(String caseNo, Long viewerAccountId) {
        DisputeCaseEntity caseEntity = disputeCaseMapper.selectByCaseNo(caseNo);
        if (caseEntity == null) {
            throw new BizException(ErrorCodeEnum.DISPUTE_SUBMIT_FAILED, "争议案件不存在: " + caseNo);
        }
        // 归属校验：仅申请人、被申请人或管理员/监管员可查看
        if (!caseEntity.getApplicantAccountId().equals(viewerAccountId)
                && (caseEntity.getRespondentAccountId() == null || !caseEntity.getRespondentAccountId().equals(viewerAccountId))
                && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该争议案件");
        }

        List<DisputeEvidenceEntity> evidences = disputeEvidenceMapper.selectByCaseId(caseEntity.getId());
        List<DisputeProcessRecordEntity> processRecords =
                disputeProcessRecordMapper.selectByCaseId(caseEntity.getId());

        return toCaseVO(caseEntity, evidences, processRecords);
    }

    /**
     * 内部调用：查询争议详情（无归属校验）
     */
    private DisputeCaseVO getDisputeDetailInternal(String caseNo) {
        DisputeCaseEntity caseEntity = disputeCaseMapper.selectByCaseNo(caseNo);
        if (caseEntity == null) {
            throw new BizException(ErrorCodeEnum.DISPUTE_SUBMIT_FAILED, "争议案件不存在: " + caseNo);
        }

        // 查询证据列表
        List<DisputeEvidenceEntity> evidences = disputeEvidenceMapper.selectByCaseId(caseEntity.getId());
        // 查询处理记录列表
        List<DisputeProcessRecordEntity> processRecords =
                disputeProcessRecordMapper.selectByCaseId(caseEntity.getId());

        return toCaseVO(caseEntity, evidences, processRecords);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<DisputeCaseVO> listDisputes(Long accountId, String status, PageQuery query) {
        Page<DisputeCaseEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<DisputeCaseEntity> wrapper = new LambdaQueryWrapper<DisputeCaseEntity>()
                .eq(status != null, DisputeCaseEntity::getStatus, status)
                .and(accountId != null, w -> w
                        .eq(DisputeCaseEntity::getApplicantAccountId, accountId)
                        .or()
                        .eq(DisputeCaseEntity::getRespondentAccountId, accountId))
                .orderByDesc(DisputeCaseEntity::getCreatedAt);
        Page<DisputeCaseEntity> result = disputeCaseMapper.selectPage(page, wrapper);

        List<DisputeCaseVO> records = result.getRecords().stream()
                .map(entity -> toCaseVO(entity, null, null))
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * 校验状态流转合法性
     *
     * @param currentStatus  当前状态
     * @param requiredStatus 要求的源状态
     * @param actionName     操作名称
     */
    private void validateTransition(String currentStatus, String requiredStatus, String actionName) {
        if (!requiredStatus.equals(currentStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_TRANSITION_DENIED,
                    "当前状态不允许执行" + actionName, null, currentStatus);
        }
    }

    /**
     * 创建证据记录
     *
     * @param caseId             案件ID
     * @param caseNo             案件编号
     * @param submitterAccountId 提交人账户ID
     * @param evidenceType       证据类型
     * @param fileUrl            文件地址
     * @param description        证据描述
     * @param now                当前时间
     */
    private void createEvidenceRecord(Long caseId, String caseNo, Long submitterAccountId,
                                       String evidenceType, String fileUrl, String description,
                                       String fileHash, LocalDateTime now) {
        ValidatedEvidenceFile validatedEvidence = validateEvidenceFile(fileUrl, fileHash, submitterAccountId);
        DisputeEvidenceEntity evidence = new DisputeEvidenceEntity();
        evidence.setCaseId(caseId);
        evidence.setCaseNo(caseNo);
        evidence.setSubmitterAccountId(submitterAccountId);
        evidence.setEvidenceType(evidenceType);
        evidence.setFileUrl(validatedEvidence.fileUrl());
        evidence.setEvidenceDescription(description);
        // 证据哈希必须来自文件内容，不可退化为URL哈希
        evidence.setFileHash(validatedEvidence.fileHash());
        evidence.setSubmitTime(now);
        evidence.setCreatedAt(now);
        evidence.setUpdatedAt(now);
        disputeEvidenceMapper.insert(evidence);
    }

    /**
     * 创建处理记录
     *
     * @param caseId     案件ID
     * @param caseNo     案件编号
     * @param operatorId 操作人ID
     * @param action     处理动作
     * @param result     处理结果
     * @param comment    处理意见
     * @param reasonCode 原因码
     * @param now        当前时间
     */
    private void createProcessRecord(Long caseId, String caseNo, Long operatorId,
                                      String action, String result, String comment,
                                      String reasonCode, LocalDateTime now) {
        DisputeProcessRecordEntity record = new DisputeProcessRecordEntity();
        record.setCaseId(caseId);
        record.setCaseNo(caseNo);
        record.setOperatorId(operatorId);
        record.setAction(action);
        record.setActionResult(result);
        record.setComment(comment);
        record.setReasonCode(reasonCode);
        record.setProcessTime(now);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        disputeProcessRecordMapper.insert(record);
    }

    /**
     * 提交争议结论上链存证
     *
     * @param caseEntity 案件实体
     * @param conclusion 结论状态
     * @param closeTime  结案时间
     */
    private void submitDisputeConclusionToChain(DisputeCaseEntity caseEntity,
                                                 String conclusion, LocalDateTime closeTime) {
        try {
            ChainSubmitResult chainResult = regulatoryChainAdapter.registerDisputeConclusion(
                    caseEntity.getId(), caseEntity.getCaseNo(),
                    conclusion, caseEntity.getResultSummary(), closeTime);
            if (chainResult.isSuccess()) {
                caseEntity.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                caseEntity.setTxHash(chainResult.getTxHash());
                caseEntity.setBlockHeight(chainResult.getBlockHeight());
                log.info("争议结论链上交易已提交待回执，caseNo={}, txHash={}",
                        caseEntity.getCaseNo(), chainResult.getTxHash());
            } else {
                caseEntity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                log.warn("争议结论上链失败，caseNo={}, reason={}",
                        caseEntity.getCaseNo(), chainResult.getFailReason());
            }
            caseEntity.setUpdatedAt(DateTimeUtil.nowUtc());
            disputeCaseMapper.updateById(caseEntity);
        } catch (Exception e) {
            caseEntity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            caseEntity.setUpdatedAt(DateTimeUtil.nowUtc());
            disputeCaseMapper.updateById(caseEntity);
            log.error("争议结论上链异常，caseNo={}", caseEntity.getCaseNo(), e);
        }
    }

    /**
     * 案件实体转视图对象（含证据和处理记录）
     *
     * @param entity         案件实体
     * @param evidences      证据列表（可为null，列表查询时不加载）
     * @param processRecords 处理记录列表（可为null，列表查询时不加载）
     * @return 争议案件视图对象
     */
    private DisputeCaseVO toCaseVO(DisputeCaseEntity entity,
                                    List<DisputeEvidenceEntity> evidences,
                                    List<DisputeProcessRecordEntity> processRecords) {
        DisputeCaseVO vo = new DisputeCaseVO();

        var basic = new DisputeCaseVO.BasicInfo();
        basic.setCaseNo(entity.getCaseNo());
        basic.setDisputeType(entity.getDisputeType());
        basic.setDescription(entity.getDescription());
        basic.setResultSummary(entity.getResultSummary());
        vo.setBasicInfo(basic);

        var status = new DisputeCaseVO.StatusInfo();
        status.setStatus(entity.getStatus());
        vo.setStatusInfo(status);

        var time = new DisputeCaseVO.TimeInfo();
        time.setSubmitTime(entity.getSubmitTime());
        time.setAcceptTime(entity.getAcceptTime());
        time.setCloseTime(entity.getCloseTime());
        time.setCreatedAt(entity.getCreatedAt());
        vo.setTimeInfo(time);

        var relation = new DisputeCaseVO.RelationInfo();
        relation.setOrderNo(entity.getOrderNo());
        relation.setWorkNo(entity.getWorkNo());
        if (entity.getApplicantAccountId() != null) {
            AccountEntity applicant = accountMapper.selectById(entity.getApplicantAccountId());
            relation.setApplicantAccountNo(applicant != null ? applicant.getAccountNo() : null);
        }
        if (entity.getRespondentAccountId() != null) {
            AccountEntity respondent = accountMapper.selectById(entity.getRespondentAccountId());
            relation.setRespondentAccountNo(respondent != null ? respondent.getAccountNo() : null);
        }
        vo.setRelationInfo(relation);

        var chain = new DisputeCaseVO.ChainInfo();
        chain.setChainStatus(entity.getChainStatus());
        chain.setTxHash(entity.getTxHash());
        chain.setBlockHeight(entity.getBlockHeight());
        vo.setChainInfo(chain);

        if (evidences != null) {
            basic.setEvidences(evidences.stream().map(e -> toEvidenceVO(e, entity)).collect(Collectors.toList()));
        }
        if (processRecords != null) {
            basic.setProcessRecords(processRecords.stream().map(r -> toRecordVO(r, entity)).collect(Collectors.toList()));
        }

        vo.setAllowedActions(List.of());

        // 统一可见性装配
        DisputeVoAssembler.applyVisibility(vo, accountId -> resolveDisputeRole(entity, accountId));

        return vo;
    }

    /**
     * 证据实体转视图对象
     *
     * @param entity 证据实体
     * @return 证据视图对象
     */
    private DisputeEvidenceVO toEvidenceVO(DisputeEvidenceEntity entity, DisputeCaseEntity caseEntity) {
        DisputeEvidenceVO vo = new DisputeEvidenceVO();
        vo.setSubmitterRole(resolveDisputeRole(caseEntity, entity.getSubmitterAccountId()));
        vo.setEvidenceType(entity.getEvidenceType());
        vo.setEvidenceDescription(entity.getEvidenceDescription());
        vo.setFileUrl(entity.getFileUrl());
        vo.setFileHash(entity.getFileHash());
        vo.setSubmitTime(entity.getSubmitTime());
        return vo;
    }

    /**
     * 处理记录实体转视图对象
     *
     * @param entity 处理记录实体
     * @return 处理记录视图对象
     */
    private ProcessRecordVO toRecordVO(DisputeProcessRecordEntity entity, DisputeCaseEntity caseEntity) {
        ProcessRecordVO vo = new ProcessRecordVO();
        vo.setOperatorRole(resolveDisputeRole(caseEntity, entity.getOperatorId()));
        vo.setAction(entity.getAction());
        vo.setActionResult(entity.getActionResult());
        vo.setComment(entity.getComment());
        vo.setProcessTime(entity.getProcessTime());
        return vo;
    }

    private String resolveDisputeRole(DisputeCaseEntity caseEntity, Long accountId) {
        if (accountId == null) return "UNKNOWN";
        if (accountId.equals(caseEntity.getApplicantAccountId())) return "APPLICANT";
        if (accountId.equals(caseEntity.getRespondentAccountId())) return "RESPONDENT";
        return "OPERATOR";
    }

    /**
     * {@inheritDoc}
     * <p>
     * 不按申请人/被申请人归属过滤，面向监管视角按条件分页查询全部案件。
     * 默认按创建时间倒序排列。列表不加载完整证据和处理记录。
     * </p>
     */
    @Override
    public PageResult<RegulatorDisputeListVO> listRegulatorDisputes(RegulatorDisputeQuery regulatorQuery, PageQuery pageQuery) {
        // 如果查询条件包含 accountNo，先解析为 accountId
        Long applicantAccountId = resolveAccountId(regulatorQuery.getApplicantAccountNo());
        Long respondentAccountId = resolveAccountId(regulatorQuery.getRespondentAccountNo());

        // 如果提供了 accountNo 但解析失败（用户不存在），直接返回空结果
        if (regulatorQuery.getApplicantAccountNo() != null && !regulatorQuery.getApplicantAccountNo().isBlank()
                && applicantAccountId == null) {
            return PageResult.of(List.of(), 0, pageQuery.getPageNo(), pageQuery.getPageSize());
        }
        if (regulatorQuery.getRespondentAccountNo() != null && !regulatorQuery.getRespondentAccountNo().isBlank()
                && respondentAccountId == null) {
            return PageResult.of(List.of(), 0, pageQuery.getPageNo(), pageQuery.getPageSize());
        }

        Page<DisputeCaseEntity> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        LambdaQueryWrapper<DisputeCaseEntity> wrapper = new LambdaQueryWrapper<>();

        if (regulatorQuery.getCaseNo() != null && !regulatorQuery.getCaseNo().isBlank()) {
            wrapper.like(DisputeCaseEntity::getCaseNo, regulatorQuery.getCaseNo());
        }
        if (regulatorQuery.getStatus() != null && !regulatorQuery.getStatus().isBlank()) {
            wrapper.eq(DisputeCaseEntity::getStatus, regulatorQuery.getStatus());
        }
        if (regulatorQuery.getDisputeType() != null && !regulatorQuery.getDisputeType().isBlank()) {
            wrapper.eq(DisputeCaseEntity::getDisputeType, regulatorQuery.getDisputeType());
        }
        if (regulatorQuery.getOrderNo() != null && !regulatorQuery.getOrderNo().isBlank()) {
            wrapper.eq(DisputeCaseEntity::getOrderNo, regulatorQuery.getOrderNo());
        }
        if (regulatorQuery.getWorkNo() != null && !regulatorQuery.getWorkNo().isBlank()) {
            wrapper.eq(DisputeCaseEntity::getWorkNo, regulatorQuery.getWorkNo());
        }
        if (applicantAccountId != null) {
            wrapper.eq(DisputeCaseEntity::getApplicantAccountId, applicantAccountId);
        }
        if (respondentAccountId != null) {
            wrapper.eq(DisputeCaseEntity::getRespondentAccountId, respondentAccountId);
        }
        if (regulatorQuery.getDateFrom() != null) {
            wrapper.ge(DisputeCaseEntity::getCreatedAt, regulatorQuery.getDateFrom());
        }
        if (regulatorQuery.getDateTo() != null) {
            wrapper.le(DisputeCaseEntity::getCreatedAt, regulatorQuery.getDateTo());
        }
        wrapper.orderByDesc(DisputeCaseEntity::getCreatedAt);

        page = disputeCaseMapper.selectPage(page, wrapper);

        List<RegulatorDisputeListVO> records = page.getRecords().stream()
                .map(this::toRegulatorDisputeListVO)
                .toList();
        return PageResult.of(records, page.getTotal(), pageQuery.getPageNo(), pageQuery.getPageSize());
    }

    private RegulatorDisputeListVO toRegulatorDisputeListVO(DisputeCaseEntity entity) {
        RegulatorDisputeListVO vo = new RegulatorDisputeListVO();
        vo.setCaseNo(entity.getCaseNo());
        vo.setStatus(entity.getStatus());
        vo.setDisputeType(entity.getDisputeType());
        vo.setOrderNo(entity.getOrderNo());
        vo.setWorkNo(entity.getWorkNo());
        vo.setSubmitTime(entity.getSubmitTime());
        vo.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getApplicantAccountId() != null) {
            AccountEntity applicant = accountMapper.selectById(entity.getApplicantAccountId());
            vo.setApplicantAccountNo(applicant != null ? applicant.getAccountNo() : null);
        }
        if (entity.getRespondentAccountId() != null) {
            AccountEntity respondent = accountMapper.selectById(entity.getRespondentAccountId());
            vo.setRespondentAccountNo(respondent != null ? respondent.getAccountNo() : null);
        }
        return vo;
    }

    private Long resolveAccountId(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return null;
        }
        AccountEntity account = accountMapper.selectByAccountNo(accountNo);
        return account != null ? account.getId() : null;
    }

    /**
     * 校验证据文件必须来源于平台附件，并与平台记录保持一致。
     */
    private ValidatedEvidenceFile validateEvidenceFile(String fileUrl, String fileHash, Long submitterAccountId) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "证据文件地址不能为空");
        }

        boolean privilegedViewer = FieldVisibilityUtil.isPrivilegedViewer();
        LambdaQueryWrapper<SysAttachmentEntity> wrapper = new LambdaQueryWrapper<SysAttachmentEntity>()
                .eq(SysAttachmentEntity::getFileUrl, fileUrl)
                .eq(!privilegedViewer, SysAttachmentEntity::getUploaderId, submitterAccountId)
                .orderByDesc(SysAttachmentEntity::getId)
                .last("limit 1");
        SysAttachmentEntity attachment = attachmentMapper.selectOne(wrapper);
        if (attachment == null) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "证据文件必须来源于平台已登记的附件");
        }
        if (attachment.getFilePath() == null || attachment.getFilePath().isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "证据文件缺少平台存储路径，无法校验");
        }

        try {
            storageService.statObject(attachment.getFilePath());
        } catch (BizException e) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "证据文件不存在、已失效或不可访问");
        }

        if (attachment.getFileHash() == null || attachment.getFileHash().isBlank()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "证据文件缺少平台生成的内容哈希，暂不支持提交");
        }
        if (fileHash != null && !fileHash.isBlank()
                && !attachment.getFileHash().equalsIgnoreCase(fileHash.trim())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "证据文件哈希与平台记录不一致");
        }

        return new ValidatedEvidenceFile(attachment.getFileUrl(), attachment.getFileHash());
    }

    /**
     * 证据文件校验通过后的标准化结果。
     */
    private record ValidatedEvidenceFile(String fileUrl, String fileHash) {
    }

}
