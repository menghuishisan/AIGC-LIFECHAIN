package com.lifechain.regulator.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.regulator.entity.RiskEventEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 风险事件数据访问层
 * <p>
 * 提供风险事件表的基础CRUD操作，以及按风险编号查询和待处理事件查询的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface RiskEventMapper extends BaseMapper<RiskEventEntity> {

    /**
     * 根据风险编号查询风险事件
     *
     * @param riskNo 风险编号
     * @return 风险事件实体，不存在则返回null
     */
    default RiskEventEntity selectByRiskNo(String riskNo) {
        return selectOne(new LambdaQueryWrapper<RiskEventEntity>()
                .eq(RiskEventEntity::getRiskNo, riskNo));
    }

    /**
     * 查询待处理的风险事件（状态为已标记或审查中）
     *
     * @return 待处理风险事件列表
     */
    default List<RiskEventEntity> selectPendingEvents() {
        return selectList(new LambdaQueryWrapper<RiskEventEntity>()
                .in(RiskEventEntity::getStatus, "RISK_MARKED", "RISK_REVIEWING")
                .orderByDesc(RiskEventEntity::getCreatedAt));
    }

    /**
     * 根据目标类型和目标ID查询风险事件
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 风险事件列表
     */
    default List<RiskEventEntity> selectByTarget(String targetType, Long targetId) {
        return selectList(new LambdaQueryWrapper<RiskEventEntity>()
                .eq(RiskEventEntity::getTargetType, targetType)
                .eq(RiskEventEntity::getTargetId, targetId)
                .orderByDesc(RiskEventEntity::getCreatedAt));
    }
}
