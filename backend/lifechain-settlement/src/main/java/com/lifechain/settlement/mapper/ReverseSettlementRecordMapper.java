package com.lifechain.settlement.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.settlement.entity.ReverseSettlementRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 逆分账记录数据访问层
 * <p>
 * 提供逆分账记录表的基础CRUD操作，以及按逆分账编号和原结算编号查询的方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface ReverseSettlementRecordMapper extends BaseMapper<ReverseSettlementRecordEntity> {

    /**
     * 根据逆分账编号查询
     *
     * @param reverseNo 逆分账编号
     * @return 逆分账记录实体，不存在则返回null
     */
    default ReverseSettlementRecordEntity selectByReverseNo(@Param("reverseNo") String reverseNo) {
        return selectOne(new LambdaQueryWrapper<ReverseSettlementRecordEntity>()
                .eq(ReverseSettlementRecordEntity::getReverseNo, reverseNo));
    }

    /**
     * 根据原结算编号查询逆分账记录
     *
     * @param settleNo 原结算编号
     * @return 逆分账记录实体，不存在则返回null
     */
    default ReverseSettlementRecordEntity selectBySettleNo(@Param("settleNo") String settleNo) {
        return selectOne(new LambdaQueryWrapper<ReverseSettlementRecordEntity>()
                .eq(ReverseSettlementRecordEntity::getSettleNo, settleNo)
                .orderByDesc(ReverseSettlementRecordEntity::getCreatedAt)
                .last("LIMIT 1"));
    }
}
