package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.trade.entity.PaymentRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 支付记录数据访问层
 * <p>
 * 提供支付记录表的基础CRUD操作及常用查询方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecordEntity> {

    /**
     * 根据支付编号查询
     *
     * @param paymentNo 支付编号
     * @return 支付记录实体，不存在则返回null
     */
    default PaymentRecordEntity selectByPaymentNo(@Param("paymentNo") String paymentNo) {
        return selectOne(new LambdaQueryWrapper<PaymentRecordEntity>()
                .eq(PaymentRecordEntity::getPaymentNo, paymentNo));
    }

    /**
     * 根据订单ID查询支付记录列表
     *
     * @param orderId 订单ID
     * @return 支付记录列表
     */
    default List<PaymentRecordEntity> selectByOrderId(@Param("orderId") Long orderId) {
        return selectList(new LambdaQueryWrapper<PaymentRecordEntity>()
                .eq(PaymentRecordEntity::getOrderId, orderId)
                .orderByDesc(PaymentRecordEntity::getCreatedAt));
    }
}
