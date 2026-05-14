package com.lifechain.auth.audit;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;

/**
 * 审计服务接口
 * <p>
 * 提供审计日志写入和状态变更历史记录的统一方法，
 * 所有关键业务操作和状态流转均需通过此服务记录。
 * </p>
 *
 * @author LifeChain
 */
public interface AuditService {

    /**
     * 写入审计日志
     *
     * @param targetType   目标类型（如 ACCOUNT、DID）
     * @param targetId     目标ID
     * @param targetNo     目标编号
     * @param action       操作动作
     * @param actionDetail 操作详情
     * @param operatorId   操作人ID
     * @param operatorRole 操作人角色
     * @param operatorIp   操作人IP
     * @param result       操作结果（SUCCESS/FAIL）
     * @param reasonCode   原因码
     */
    void writeAuditLog(String targetType, Long targetId, String targetNo,
                       String action, String actionDetail,
                       Long operatorId, String operatorRole, String operatorIp,
                       String result, String reasonCode);

    /**
     * 写入状态变更历史
     *
     * @param bizType      业务类型
     * @param bizId        业务ID
     * @param bizNo        业务编号
     * @param fromStatus   原状态
     * @param toStatus     目标状态
     * @param changeReason 变更原因
     * @param reasonCode   原因码
     * @param operatorId   操作人ID
     */
    void writeStatusHistory(String bizType, Long bizId, String bizNo,
                            String fromStatus, String toStatus,
                            String changeReason, String reasonCode, Long operatorId);

    /**
     * 分页查询审计日志
     */
    PageResult<AuditLogVO> listAuditLogs(String targetType, String action, PageQuery query);

    /**
     * 分页查询状态变更历史
     */
    PageResult<StatusHistoryVO> listStatusHistory(String bizType, String bizNo, PageQuery query);
}
