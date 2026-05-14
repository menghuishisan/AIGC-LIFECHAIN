package com.lifechain.auth.audit;

/**
 * 统一溯源事件服务接口
 * <p>
 * 所有关键业务生命周期动作同步写入 trace_event，
 * 支持按业务编号查询完整轨迹。
 * </p>
 *
 * @author LifeChain
 */
public interface TraceEventService {

    /**
     * 写入溯源事件
     *
     * @param bizType          业务类型（WORK/ORDER/SETTLEMENT/LICENSE/CLAIM/CERTIFICATE/DISPUTE/FREEZE）
     * @param bizId            业务ID
     * @param bizNo            业务编号
     * @param eventType        事件类型（如 WORK_UPLOADED、ORDER_CREATED、CLAIM_SUBMITTED 等）
     * @param eventDescription 事件描述
     * @param operatorId       操作人ID
     * @param operatorRole     操作人角色
     * @param extraData        扩展数据（JSON格式，可为null）
     */
    void writeTraceEvent(String bizType, Long bizId, String bizNo,
                         String eventType, String eventDescription,
                         Long operatorId, String operatorRole, String extraData);
}
