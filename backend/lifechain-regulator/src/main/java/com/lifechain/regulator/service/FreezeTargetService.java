package com.lifechain.regulator.service;

/**
 * 冻结目标联动服务
 * <p>
 * 冻结/解冻操作时联动推进目标业务对象状态。
 * </p>
 */
public interface FreezeTargetService {

    /**
     * 冻结目标对象 —— 推进目标到冻结态
     */
    void freezeTarget(String targetType, String targetNo, String freezeNo);

    /**
     * 解冻目标对象 —— 恢复目标到正常态
     */
    void unfreezeTarget(String targetType, String targetNo, String previousStatus, String freezeNo);

    /**
     * 获取目标对象当前业务状态，用于冻结快照。
     */
    String captureCurrentStatus(String targetType, String targetNo);

    /**
     * 根据目标类型和目标编号解析内部主键
     */
    Long resolveTargetId(String targetType, String targetNo);

    /**
     * 根据目标类型和目标编号解析应接收通知的账户ID。
     */
    Long resolveTargetAccountId(String targetType, String targetNo);
}
