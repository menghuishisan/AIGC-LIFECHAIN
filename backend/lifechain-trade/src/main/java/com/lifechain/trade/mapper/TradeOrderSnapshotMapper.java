package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.trade.entity.TradeOrderSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 交易订单快照数据访问层
 * <p>
 * 提供订单快照表的基础CRUD操作及根据订单ID查询快照列表的方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface TradeOrderSnapshotMapper extends BaseMapper<TradeOrderSnapshotEntity> {

    /**
     * 根据订单ID查询所有快照
     *
     * @param orderId 订单ID
     * @return 快照列表
     */
    default List<TradeOrderSnapshotEntity> selectByOrderId(@Param("orderId") Long orderId) {
        return selectList(new LambdaQueryWrapper<TradeOrderSnapshotEntity>()
                .eq(TradeOrderSnapshotEntity::getOrderId, orderId)
                .orderByAsc(TradeOrderSnapshotEntity::getSnapshotTime));
    }
}
