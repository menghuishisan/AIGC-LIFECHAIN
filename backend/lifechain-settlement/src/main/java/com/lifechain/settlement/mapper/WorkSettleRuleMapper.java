package com.lifechain.settlement.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.settlement.entity.WorkSettleRuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作品结算规则数据访问层
 * <p>
 * 提供作品结算规则表的基础CRUD操作，以及按作品查询生效规则的方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface WorkSettleRuleMapper extends BaseMapper<WorkSettleRuleEntity> {

    /**
     * 根据作品ID查询所有结算规则
     *
     * @param workId 作品ID
     * @return 结算规则列表
     */
    default List<WorkSettleRuleEntity> selectByWorkId(@Param("workId") Long workId) {
        return selectList(new LambdaQueryWrapper<WorkSettleRuleEntity>()
                .eq(WorkSettleRuleEntity::getWorkId, workId)
                .orderByDesc(WorkSettleRuleEntity::getCreatedAt));
    }

    /**
     * 查询作品当前生效的结算规则
     * <p>
     * 状态为 ACTIVE 且生效时间不晚于当前时间的规则，取最新一条。
     * </p>
     *
     * @param workId 作品ID
     * @return 生效的结算规则，不存在则返回null
     */
    default WorkSettleRuleEntity selectEffectiveRule(@Param("workId") Long workId) {
        return selectOne(new LambdaQueryWrapper<WorkSettleRuleEntity>()
                .eq(WorkSettleRuleEntity::getWorkId, workId)
                .eq(WorkSettleRuleEntity::getStatus, "ACTIVE")
                .orderByDesc(WorkSettleRuleEntity::getEffectiveTime)
                .last("LIMIT 1"));
    }

    /**
     * 根据作品编号查询当前生效的结算规则
     *
     * @param workNo 作品编号
     * @return 生效的结算规则，不存在则返回null
     */
    default WorkSettleRuleEntity selectEffectiveRuleByWorkNo(@Param("workNo") String workNo) {
        return selectOne(new LambdaQueryWrapper<WorkSettleRuleEntity>()
                .eq(WorkSettleRuleEntity::getWorkNo, workNo)
                .eq(WorkSettleRuleEntity::getStatus, "ACTIVE")
                .orderByDesc(WorkSettleRuleEntity::getEffectiveTime)
                .last("LIMIT 1"));
    }
}
