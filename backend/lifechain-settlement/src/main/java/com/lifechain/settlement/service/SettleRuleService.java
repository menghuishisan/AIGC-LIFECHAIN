package com.lifechain.settlement.service;

import com.lifechain.settlement.dto.BindSettleRuleRequest;
import com.lifechain.settlement.dto.WorkSettleRuleVO;

/**
 * 结算规则服务接口
 * <p>
 * 管理作品与分账规则的绑定关系，支持查询作品当前生效的结算规则。
 * </p>
 *
 * @author LifeChain
 */
public interface SettleRuleService {

    /**
     * 绑定作品结算规则
     * <p>
     * 将分账规则绑定到指定作品，验证模板有效性及比例合规性。
     * 同一作品同一时间仅允许一条 ACTIVE 规则。
     * </p>
     *
     * @param request 绑定请求
     * @return 规则视图对象
     */
    WorkSettleRuleVO bindRule(BindSettleRuleRequest request);

    /**
     * 查询作品当前生效的结算规则
     *
     * @param workNo 作品编号
     * @return 规则视图对象，无规则则返回null
     */
    WorkSettleRuleVO getEffectiveRule(String workNo);
}
