package com.lifechain.auth.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审计服务实现
 * <p>
 * 提供审计日志和状态变更历史的写入功能，所有记录时间统一使用UTC。
 * 写入操作异步友好，不影响主流程事务回滚。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogMapper auditLogMapper;
    private final StatusHistoryMapper statusHistoryMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAuditLog(String targetType, Long targetId, String targetNo,
                              String action, String actionDetail,
                              Long operatorId, String operatorRole, String operatorIp,
                              String result, String reasonCode) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setTargetNo(targetNo);
        entity.setAction(action);
        entity.setActionDetail(actionDetail);
        entity.setOperatorId(operatorId);
        entity.setOperatorRole(operatorRole);
        entity.setOperatorIp(operatorIp);
        entity.setResult(result);
        entity.setReasonCode(reasonCode);
        entity.setLogTime(DateTimeUtil.nowUtc());

        auditLogMapper.insert(entity);
        log.info("审计日志已写入，目标类型={}，目标编号={}，动作={}，结果={}", targetType, targetNo, action, result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeStatusHistory(String bizType, Long bizId, String bizNo,
                                   String fromStatus, String toStatus,
                                   String changeReason, String reasonCode, Long operatorId) {
        StatusHistoryEntity entity = new StatusHistoryEntity();
        entity.setBizType(bizType);
        entity.setBizId(bizId);
        entity.setBizNo(bizNo);
        entity.setFromStatus(fromStatus);
        entity.setToStatus(toStatus);
        entity.setChangeReason(changeReason);
        entity.setReasonCode(reasonCode);
        entity.setOperatorId(operatorId);
        entity.setChangeTime(DateTimeUtil.nowUtc());

        statusHistoryMapper.insert(entity);
        log.info("状态变更历史已写入，业务类型={}，业务编号={}，{} -> {}", bizType, bizNo, fromStatus, toStatus);
    }

    @Override
    public PageResult<AuditLogVO> listAuditLogs(String targetType, String action, PageQuery query) {
        LambdaQueryWrapper<AuditLogEntity> wrapper = new LambdaQueryWrapper<AuditLogEntity>()
                .eq(targetType != null, AuditLogEntity::getTargetType, targetType)
                .eq(action != null, AuditLogEntity::getAction, action)
                .orderByDesc(AuditLogEntity::getLogTime);
        Page<AuditLogEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<AuditLogEntity> result = auditLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords().stream().map(AuditLogVO::fromEntity).toList(),
                result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    @Override
    public PageResult<StatusHistoryVO> listStatusHistory(String bizType, String bizNo, PageQuery query) {
        LambdaQueryWrapper<StatusHistoryEntity> wrapper = new LambdaQueryWrapper<StatusHistoryEntity>()
                .eq(StatusHistoryEntity::getBizType, bizType)
                .eq(StatusHistoryEntity::getBizNo, bizNo)
                .orderByDesc(StatusHistoryEntity::getChangeTime);
        Page<StatusHistoryEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<StatusHistoryEntity> result = statusHistoryMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords().stream().map(StatusHistoryVO::fromEntity).toList(),
                result.getTotal(), query.getPageNo(), query.getPageSize());
    }
}
