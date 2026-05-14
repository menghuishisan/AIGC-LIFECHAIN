package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.trade.entity.RefundRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 退款记录数据访问层
 * <p>
 * 提供退款记录表的基础CRUD操作及常用查询方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecordEntity> {

    /**
     * 根据退款编号查询
     *
     * @param refundNo 退款编号
     * @return 退款记录实体，不存在则返回null
     */
    default RefundRecordEntity selectByRefundNo(@Param("refundNo") String refundNo) {
        return selectOne(new LambdaQueryWrapper<RefundRecordEntity>()
                .eq(RefundRecordEntity::getRefundNo, refundNo));
    }

    /**
     * 根据订单ID查询退款记录列表
     *
     * @param orderId 订单ID
     * @return 退款记录列表
     */
    default List<RefundRecordEntity> selectByOrderId(@Param("orderId") Long orderId) {
        return selectList(new LambdaQueryWrapper<RefundRecordEntity>()
                .eq(RefundRecordEntity::getOrderId, orderId)
                .orderByDesc(RefundRecordEntity::getCreatedAt));
    }
}
