package com.lifechain.regulator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.adapter.RegulatoryChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.infra.storage.StorageService;
import com.lifechain.regulator.dto.CreateReportRequest;
import com.lifechain.regulator.dto.HandleReportRequest;
import com.lifechain.regulator.dto.ReportVO;
import com.lifechain.regulator.entity.RegulatorReportEntity;
import com.lifechain.regulator.mapper.RegulatorReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 监管报告服务实现
 * <p>
 * 完整的监管报告生命周期管理实现：
 * <ol>
 *   <li>创建报告：生成编号、初始状态为DRAFT</li>
 *   <li>触发生成：DRAFT → GENERATING</li>
 *   <li>完成报告：GENERATING → COMPLETED（计算摘要哈希并上链存证）</li>
 *   <li>标记失败：GENERATING → FAILED</li>
 * </ol>
 * 已完成的报告通过摘要哈希上链存证，确保报告内容的不可篡改性。
 * 所有状态变更均记录审计日志和状态变更历史。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final RegulatorReportMapper regulatorReportMapper;
    private final RegulatoryChainAdapter regulatoryChainAdapter;
    private final AuditService auditService;
    private final StorageService storageService;

    /** 报告状态常量 */
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_GENERATING = "GENERATING";
    private static final String STATUS_FAILED = "FAILED";

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportVO createReport(Long operatorId, CreateReportRequest request) {
        log.info("创建监管报告，operatorId={}, reportType={}, title={}",
                operatorId, request.getReportType(), request.getReportTitle());

        LocalDateTime now = DateTimeUtil.nowUtc();
        String reportNo = BizNoUtil.reportNo();

        RegulatorReportEntity entity = new RegulatorReportEntity();
        entity.setReportNo(reportNo);
        entity.setReportType(request.getReportType());
        entity.setReportTitle(request.getReportTitle());
        entity.setReportContent(request.getReportContent());
        entity.setReportFileUrl(request.getReportFileUrl());
        entity.setGeneratorId(operatorId);
        entity.setStatus(STATUS_DRAFT);
        entity.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        regulatorReportMapper.insert(entity);

        log.info("监管报告创建成功，reportNo={}", reportNo);

        auditService.writeAuditLog(
                BizTypeEnum.REPORT.getCode(), entity.getId(), reportNo,
                "REPORT_CREATE", "创建监管报告: " + request.getReportTitle(),
                operatorId, null, null, "SUCCESS", null);

        auditService.writeStatusHistory(
                BizTypeEnum.REPORT.getCode(), entity.getId(), reportNo,
                null, STATUS_DRAFT,
                "创建监管报告", null, operatorId);

        return toVO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportVO handleReport(Long operatorId, HandleReportRequest request) {
        log.info("处理监管报告，operatorId={}, reportNo={}, action={}",
                operatorId, request.getReportNo(), request.getAction());

        RegulatorReportEntity entity = regulatorReportMapper.selectByReportNo(request.getReportNo());
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.REPORT_GENERATE_FAILED,
                    "监管报告不存在: " + request.getReportNo());
        }

        String fromStatus = entity.getStatus();
        LocalDateTime now = DateTimeUtil.nowUtc();
        String toStatus;

        switch (request.getAction().toUpperCase()) {
            case "GENERATE" -> {
                if (!STATUS_DRAFT.equals(fromStatus)) {
                    throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                            "仅草稿状态可触发生成", null, fromStatus);
                }
                toStatus = STATUS_GENERATING;
                entity.setGenerateTime(now);
            }
            case "COMPLETE" -> {
                if (!STATUS_GENERATING.equals(fromStatus)) {
                    throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                            "仅生成中状态可标记完成", null, fromStatus);
                }
                // 先保持GENERATING状态，等链上回执确认后再由回执处理器推到COMPLETED
                toStatus = STATUS_GENERATING;
                // 生成报告文件并上传对象存储
                String reportFileContent = buildReportFileContent(entity, now);
                String objectName = "reports/" + entity.getReportNo() + "/report.txt";
                String fileUrl = storageService.uploadFile(objectName,
                        reportFileContent.getBytes(StandardCharsets.UTF_8), "text/plain");
                entity.setReportFileUrl(fileUrl);
                // 计算摘要哈希
                String hashContent = entity.getReportNo() + ":" + entity.getReportTitle()
                        + ":" + entity.getReportContent();
                entity.setSummaryHash(HashUtil.sha256(hashContent));
            }
            case "FAIL" -> {
                if (!STATUS_GENERATING.equals(fromStatus)) {
                    throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                            "仅生成中状态可标记失败", null, fromStatus);
                }
                toStatus = STATUS_FAILED;
            }
            default -> throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "不支持的处理动作: " + request.getAction());
        }

        entity.setStatus(toStatus);
        entity.setUpdatedAt(now);
        regulatorReportMapper.updateById(entity);

        log.info("监管报告处理完成，reportNo={}, fromStatus={}, toStatus={}",
                request.getReportNo(), fromStatus, toStatus);

        // 完成时提交链上存证（action=COMPLETE时触发上链）
        if ("COMPLETE".equalsIgnoreCase(request.getAction())) {
            submitReportToChain(entity, now);
        }

        auditService.writeStatusHistory(
                BizTypeEnum.REPORT.getCode(), entity.getId(), entity.getReportNo(),
                fromStatus, toStatus,
                "处理监管报告: " + request.getAction(), null, operatorId);

        return toVO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<ReportVO> listReports(String reportType, String status, PageQuery query) {
        Page<RegulatorReportEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<RegulatorReportEntity> wrapper = new LambdaQueryWrapper<RegulatorReportEntity>()
                .eq(reportType != null, RegulatorReportEntity::getReportType, reportType)
                .eq(status != null, RegulatorReportEntity::getStatus, status)
                .orderByDesc(RegulatorReportEntity::getCreatedAt);
        Page<RegulatorReportEntity> result = regulatorReportMapper.selectPage(page, wrapper);

        List<ReportVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReportVO getByReportNo(String reportNo) {
        RegulatorReportEntity entity = regulatorReportMapper.selectByReportNo(reportNo);
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.REPORT_GENERATE_FAILED, "监管报告不存在: " + reportNo);
        }
        return toVO(entity);
    }

    /**
     * 提交报告上链存证
     *
     * @param entity       报告实体
     * @param generateTime 生成时间
     */
    private void submitReportToChain(RegulatorReportEntity entity, LocalDateTime generateTime) {
        try {
            // 提交前置为GENERATING状态，等待回执确认
            entity.setStatus(STATUS_GENERATING);
            ChainSubmitResult chainResult = regulatoryChainAdapter.registerReport(
                    entity.getId(), entity.getReportNo(),
                    entity.getSummaryHash(), generateTime);
            if (chainResult.isSuccess()) {
                entity.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                entity.setTxHash(chainResult.getTxHash());
                entity.setBlockHeight(chainResult.getBlockHeight());
                log.info("报告上链提交成功（待回执），reportNo={}, txHash={}", entity.getReportNo(), chainResult.getTxHash());
            } else {
                entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                entity.setStatus(STATUS_FAILED);
                log.warn("报告上链失败，reportNo={}, reason={}", entity.getReportNo(), chainResult.getFailReason());
            }
            entity.setUpdatedAt(DateTimeUtil.nowUtc());
            regulatorReportMapper.updateById(entity);
        } catch (Exception e) {
            entity.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            entity.setStatus(STATUS_FAILED);
            entity.setUpdatedAt(DateTimeUtil.nowUtc());
            regulatorReportMapper.updateById(entity);
            log.error("报告上链异常，reportNo={}", entity.getReportNo(), e);
        }
    }

    /**
     * 构建报告文件内容
     */
    private String buildReportFileContent(RegulatorReportEntity entity, LocalDateTime generateTime) {
        return "========== 监管报告 ==========\n"
                + "报告编号: " + entity.getReportNo() + "\n"
                + "报告类型: " + entity.getReportType() + "\n"
                + "报告标题: " + entity.getReportTitle() + "\n"
                + "生成时间: " + DateTimeUtil.formatUtc(generateTime) + "\n"
                + "================================\n\n"
                + entity.getReportContent() + "\n\n"
                + "================================\n"
                + "摘要哈希: " + entity.getSummaryHash() + "\n"
                + "生成人ID: " + entity.getGeneratorId() + "\n";
    }

    /**
     * 实体转视图对象
     *
     * @param entity 监管报告实体
     * @return 监管报告视图对象
     */
    private ReportVO toVO(RegulatorReportEntity entity) {
        ReportVO vo = new ReportVO();
        vo.setReportNo(entity.getReportNo());
        vo.setReportType(entity.getReportType());
        vo.setReportTitle(entity.getReportTitle());
        vo.setReportContent(entity.getReportContent());
        vo.setReportFileUrl(entity.getReportFileUrl());
        vo.setStatus(entity.getStatus());
        vo.setGenerateTime(entity.getGenerateTime());
        vo.setSummaryHash(entity.getSummaryHash());
        vo.setChainStatus(entity.getChainStatus());
        vo.setTxHash(entity.getTxHash());
        vo.setBlockHeight(entity.getBlockHeight());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
