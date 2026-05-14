package com.lifechain.regulator.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.FreezeRecordVO;
import com.lifechain.regulator.dto.FreezeRequest;
import com.lifechain.regulator.dto.UnfreezeRequest;

/**
 * 冻结服务接口
 * <p>
 * 提供冻结/解冻操作的完整生命周期管理。
 * 冻结操作分为审核冻结（REVIEW_REQUIRED）和监管直接冻结（REGULATOR_DIRECT）两种模式。
 * 冻结信息同步上链存证，确保监管操作的不可篡改性和可追溯性。
 * 所有状态变更均记录审计日志和状态变更历史。
 * </p>
 *
 * @author LifeChain
 */
public interface FreezeService {

    /**
     * 发起冻结
     * <p>
     * 校验目标未被冻结，生成冻结编号，创建冻结记录。
     * 监管直接冻结模式下直接生效并提交链上存证；
     * 审核冻结模式下需后续审批生效。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    冻结请求
     * @return 冻结记录视图对象
     */
    FreezeRecordVO freeze(Long operatorId, FreezeRequest request);

    /**
     * 解冻操作
     * <p>
     * 校验目标处于已批准冻结状态，更新为解冻状态，记录解冻原因和时间。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    解冻请求
     * @return 更新后的冻结记录视图对象
     */
    FreezeRecordVO unfreeze(Long operatorId, UnfreezeRequest request);

    /**
     * 根据冻结编号查询冻结记录详情
     *
     * @param freezeNo 冻结编号
     * @return 冻结记录视图对象
     */
    FreezeRecordVO getFreezeRecord(String freezeNo);

    /**
     * 分页查询冻结记录列表
     *
     * @param targetType 目标类型（可选）
     * @param status     冻结状态（可选）
     * @param query      分页参数
     * @return 分页冻结记录列表
     */
    PageResult<FreezeRecordVO> listFreezeRecords(String targetType, String status, PageQuery query);

    /**
     * 事后复核直接冻结
     * <p>
     * 对紧急直接冻结进行事后复核，只有 PENDING_POST_REVIEW 状态的记录可复核。
     * 复核通过则标记为 REVIEW_PASSED；复核不通过则自动解冻。
     * </p>
     *
     * @param operatorId  复核人ID
     * @param freezeNo    冻结编号
     * @param approved    是否通过复核
     * @param reviewNote  复核意见
     * @return 更新后的冻结记录
     */
    FreezeRecordVO reviewFreeze(Long operatorId, String freezeNo, boolean approved, String reviewNote);
}
