package com.lifechain.settlement.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.settlement.entity.SettlementItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结算明细数据访问层
 * <p>
 * 提供结算明细表的基础CRUD操作，以及按结算记录ID和结算编号查询明细列表的方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface SettlementItemMapper extends BaseMapper<SettlementItemEntity> {

    /**
     * 根据结算记录ID查询所有明细
     *
     * @param settleId 结算记录ID
     * @return 结算明细列表
     */
    default List<SettlementItemEntity> selectBySettleId(@Param("settleId") Long settleId) {
        return selectList(new LambdaQueryWrapper<SettlementItemEntity>()
                .eq(SettlementItemEntity::getSettleId, settleId)
                .orderByAsc(SettlementItemEntity::getRoleType));
    }

    /**
     * 根据结算编号查询所有明细
     *
     * @param settleNo 结算编号
     * @return 结算明细列表
     */
    default List<SettlementItemEntity> selectBySettleNo(@Param("settleNo") String settleNo) {
        return selectList(new LambdaQueryWrapper<SettlementItemEntity>()
                .eq(SettlementItemEntity::getSettleNo, settleNo)
                .orderByAsc(SettlementItemEntity::getRoleType));
    }
}
