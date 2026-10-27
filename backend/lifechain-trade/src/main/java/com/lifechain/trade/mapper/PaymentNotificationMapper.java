package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.trade.entity.PaymentNotificationEntity;
import org.apache.ibatis.annotations.Mapper;

/** 支付通知随订单事务提交；唯一索引和订单行锁共同保证多实例幂等。 */
@Mapper
public interface PaymentNotificationMapper extends BaseMapper<PaymentNotificationEntity> {
    default PaymentNotificationEntity selectByNotifyId(String channel, String notifyId) {
        return selectOne(new LambdaQueryWrapper<PaymentNotificationEntity>()
                .eq(PaymentNotificationEntity::getPayChannel, channel)
                .eq(PaymentNotificationEntity::getNotifyId, notifyId));
    }
}
