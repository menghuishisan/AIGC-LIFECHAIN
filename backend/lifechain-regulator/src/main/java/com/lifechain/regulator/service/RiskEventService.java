package com.lifechain.regulator.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.CreateRiskEventRequest;
import com.lifechain.regulator.dto.HandleRiskEventRequest;
import com.lifechain.regulator.dto.RiskEventVO;

/**
 * 风险事件服务接口
 * <p>
 * 提供风险事件的创建、处理、查询等操作。
 * 风险事件由系统自动检测或管理员手动创建，处理后可触发冻结等后续操作。
 * 所有状态变更均记录审计日志和状态变更历史，确保全链路可追溯。
 * </p>
 *
 * @author LifeChain
 */
public interface RiskEventService {

    /**
     * 创建风险事件
     * <p>
     * 生成风险编号，初始状态为RISK_MARKED，记录报告人信息和报告时间。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    创建请求
     * @return 风险事件视图对象
     */
    RiskEventVO createRiskEvent(Long operatorId, CreateRiskEventRequest request);

    /**
     * 处理风险事件
     * <p>
     * 支持的处理动作：
     * <ul>
     *   <li>CONFIRM: 确认风险 → RISK_CONFIRMED</li>
     *   <li>RELEASE: 释放风险 → RISK_RELEASED</li>
     *   <li>FREEZE: 冻结目标 → RISK_FROZEN</li>
     *   <li>REVIEW: 进入审查 → RISK_REVIEWING</li>
     * </ul>
     * 仅允许对RISK_MARKED或RISK_REVIEWING状态的事件进行处理。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    处理请求
     * @return 更新后的风险事件视图对象
     */
    RiskEventVO handleRiskEvent(Long operatorId, HandleRiskEventRequest request);

    /**
     * 分页查询待处理的风险事件
     *
     * @param query 分页参数
     * @return 分页风险事件列表
     */
    PageResult<RiskEventVO> listPendingEvents(PageQuery query);

    /**
     * 根据风险编号查询风险事件详情
     *
     * @param riskNo 风险编号
     * @return 风险事件视图对象
     */
    RiskEventVO getByRiskNo(String riskNo);
}
