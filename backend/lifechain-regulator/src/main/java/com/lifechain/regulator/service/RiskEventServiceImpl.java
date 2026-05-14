package com.lifechain.regulator.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.RiskStatusEnum;
import com.lifechain.common.enums.TargetTypeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.regulator.dto.*;
import com.lifechain.regulator.entity.RiskEventEntity;
import com.lifechain.regulator.mapper.RiskEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 风险事件服务实现
 * <p>
 * 完整的风险事件生命周期管理实现：
 * <ol>
 *   <li>创建风险事件：生成编号、初始状态为RISK_MARKED</li>
 *   <li>处理风险事件：支持确认、释放、冻结、审查等多种处置动作</li>
 *   <li>分页查询待处理事件和按编号查询详情</li>
 * </ol>
 * 所有状态变更均记录审计日志和状态变更历史，确保全链路可追溯。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEventServiceImpl implements RiskEventService {

    private final RiskEventMapper riskEventMapper;
    private final FreezeService freezeService;
    private final FreezeTargetService freezeTargetService;
    private final AuditService auditService;
    private final TraceEventService traceEventService;

    /** 允许处理的源状态 */
    private static final Set<String> HANDLEABLE_STATUSES = Set.of(
            RiskStatusEnum.RISK_MARKED.getCode(),
            RiskStatusEnum.RISK_REVIEWING.getCode()
    );

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskEventVO createRiskEvent(Long operatorId, CreateRiskEventRequest request) {
        log.info("创建风险事件，operatorId={}, targetType={}, targetNo={}",
                operatorId, request.getTargetType(), request.getTargetNo());

        TargetTypeEnum.fromCode(request.getTargetType());

        // 通过业务编号解析内部主键
        Long targetId = freezeTargetService.resolveTargetId(request.getTargetType(), request.getTargetNo());

        LocalDateTime now = DateTimeUtil.nowUtc();
        String riskNo = BizNoUtil.riskNo();

        RiskEventEntity entity = new RiskEventEntity();
        entity.setRiskNo(riskNo);
        entity.setTargetType(request.getTargetType());
        entity.setTargetId(targetId);
        entity.setTargetNo(request.getTargetNo());
        entity.setStatus(RiskStatusEnum.RISK_MARKED.getCode());
        entity.setRiskLevel(request.getRiskLevel());
        entity.setRiskType(request.getRiskType());
        entity.setRiskDescription(request.getRiskDescription());
        entity.setReporterId(operatorId);
        entity.setReportTime(now);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        riskEventMapper.insert(entity);

        log.info("风险事件创建成功，riskNo={}, targetType={}, targetNo={}",
                riskNo, request.getTargetType(), request.getTargetNo());

        auditService.writeAuditLog(
                TargetTypeEnum.fromCode(request.getTargetType()).getCode(),
                targetId, request.getTargetNo(),
                "RISK_CREATE", "创建风险事件: " + riskNo,
                operatorId, null, null, "SUCCESS", null);

        auditService.writeStatusHistory(
                BizTypeEnum.RISK.getCode(), entity.getId(), riskNo,
                null, RiskStatusEnum.RISK_MARKED.getCode(),
                "创建风险事件", null, operatorId);

        traceEventService.writeTraceEvent(BizTypeEnum.RISK.getCode(), entity.getId(), riskNo,
                "RISK_EVENT_CREATED", "风险事件已创建", operatorId, null, null);

        return toVO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskEventVO handleRiskEvent(Long operatorId, HandleRiskEventRequest request) {
        log.info("处理风险事件，operatorId={}, riskNo={}, action={}",
                operatorId, request.getRiskNo(), request.getAction());

        RiskEventEntity entity = riskEventMapper.selectByRiskNo(request.getRiskNo());
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.RISK_MARK_FAILED, "风险事件不存在: " + request.getRiskNo());
        }
        if (!HANDLEABLE_STATUSES.contains(entity.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前状态不允许处理", null, entity.getStatus());
        }

        String fromStatus = entity.getStatus();
        LocalDateTime now = DateTimeUtil.nowUtc();
        String toStatus;

        switch (request.getAction().toUpperCase()) {
            case "CONFIRM" -> toStatus = RiskStatusEnum.RISK_CONFIRMED.getCode();
            case "RELEASE" -> toStatus = RiskStatusEnum.RISK_RELEASED.getCode();
            case "FREEZE" -> toStatus = RiskStatusEnum.RISK_FROZEN.getCode();
            case "REVIEW" -> toStatus = RiskStatusEnum.RISK_REVIEWING.getCode();
            default -> throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "不支持的处理动作: " + request.getAction());
        }

        entity.setStatus(toStatus);
        entity.setResultSummary(request.getResultSummary());
        entity.setReasonCode(request.getReasonCode());
        entity.setResolveTime(now);
        entity.setUpdatedAt(now);
        riskEventMapper.updateById(entity);

        log.info("风险事件处理完成，riskNo={}, fromStatus={}, toStatus={}",
                request.getRiskNo(), fromStatus, toStatus);

        // 风险→冻结联动：FREEZE动作时创建真实冻结记录
        if ("FREEZE".equals(request.getAction().toUpperCase())) {
            FreezeRequest freezeRequest = new FreezeRequest();
            freezeRequest.setTargetType(entity.getTargetType());
            freezeRequest.setTargetNo(entity.getTargetNo());
            freezeRequest.setFreezeMode("REGULATOR_DIRECT");
            freezeRequest.setFreezeReason("风险事件处置冻结: " + entity.getRiskNo());
            freezeRequest.setUrgentBasisNo(entity.getRiskNo());
            freezeService.freeze(operatorId, freezeRequest);
            log.info("风险→冻结联动完成，riskNo={}, targetNo={}", entity.getRiskNo(), entity.getTargetNo());
        }

        auditService.writeStatusHistory(
                BizTypeEnum.RISK.getCode(), entity.getId(), entity.getRiskNo(),
                fromStatus, toStatus,
                "处理风险事件: " + request.getAction(), request.getReasonCode(), operatorId);

        traceEventService.writeTraceEvent(BizTypeEnum.RISK.getCode(), entity.getId(), entity.getRiskNo(),
                "RISK_EVENT_HANDLED", "风险事件处理: " + request.getAction(), operatorId, null, null);

        return toVO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<RiskEventVO> listPendingEvents(PageQuery query) {
        Page<RiskEventEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<RiskEventEntity> wrapper = new LambdaQueryWrapper<RiskEventEntity>()
                .in(RiskEventEntity::getStatus, HANDLEABLE_STATUSES)
                .orderByDesc(RiskEventEntity::getCreatedAt);
        Page<RiskEventEntity> result = riskEventMapper.selectPage(page, wrapper);

        List<RiskEventVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RiskEventVO getByRiskNo(String riskNo) {
        RiskEventEntity entity = riskEventMapper.selectByRiskNo(riskNo);
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.RISK_MARK_FAILED, "风险事件不存在: " + riskNo);
        }
        return toVO(entity);
    }

    /**
     * 实体转视图对象
     *
     * @param entity 风险事件实体
     * @return 风险事件视图对象
     */
    private RiskEventVO toVO(RiskEventEntity entity) {
        RiskEventVO vo = new RiskEventVO();
        vo.setRiskNo(entity.getRiskNo());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetNo(entity.getTargetNo());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setRiskType(entity.getRiskType());
        vo.setRiskDescription(entity.getRiskDescription());
        vo.setStatus(entity.getStatus());
        vo.setResultSummary(entity.getResultSummary());
        vo.setResolveTime(entity.getResolveTime());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
