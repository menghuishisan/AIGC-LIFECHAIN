package com.lifechain.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.common.enums.OrderStatusEnum;
import com.lifechain.common.enums.PayChannelEnum;
import com.lifechain.common.enums.PayStatusEnum;
import com.lifechain.common.enums.RefundStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.trade.entity.RefundRecordEntity;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.RefundRecordMapper;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 浏览器退出或通知丢失时仍核实付款、退款和过期结果；每笔由订单服务独立事务处理。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationJob {
    private final TradeOrderMapper orderMapper;
    private final RefundRecordMapper refundMapper;
    private final OrderService orderService;
    private long orderCursor;
    private long refundCursor;

    @Scheduled(fixedDelayString = "${lifechain.pay.reconcile-delay-ms:60000}", initialDelay = 60000)
    public void reconcile() {
        var orders = orderMapper.selectPage(new Page<TradeOrderEntity>(1, 50, false),
                new LambdaQueryWrapper<TradeOrderEntity>().gt(TradeOrderEntity::getId, orderCursor)
                        .and(q -> q.eq(TradeOrderEntity::getPayChannel, PayChannelEnum.ALIPAY.getCode())
                                .eq(TradeOrderEntity::getOrderStatus, OrderStatusEnum.PAY_PENDING_CONFIRM.getCode())
                                .or(n -> n.eq(TradeOrderEntity::getOrderStatus, OrderStatusEnum.ORDER_CREATED.getCode())
                                        .le(TradeOrderEntity::getExpireTime, DateTimeUtil.nowUtc()))
                                .or(n -> n.eq(TradeOrderEntity::getOrderStatus, OrderStatusEnum.ORDER_EXCEPTION.getCode())
                                        .eq(TradeOrderEntity::getPayStatus, PayStatusEnum.PAY_SUCCESS.getCode())))
                        .orderByAsc(TradeOrderEntity::getId)).getRecords();
        for (TradeOrderEntity order : orders) {
            orderCursor = order.getId();
            try {
                if (OrderStatusEnum.ORDER_EXCEPTION.getCode().equals(order.getOrderStatus())) {
                    orderService.retryLicenseGrant(order.getOrderNo());
                } else if (order.getExpireTime() != null && !order.getExpireTime().isAfter(DateTimeUtil.nowUtc())) {
                    orderService.expireOrder(order.getOrderNo());
                } else {
                    orderService.refreshPayment(order.getBuyerAccountId(), order.getOrderNo());
                }
            } catch (Exception e) {
                log.warn("支付补偿暂未完成，下轮重试: orderNo={}", order.getOrderNo());
            }
        }
        if (orders.size() < 50) orderCursor = 0;
        var refunds = refundMapper.selectPage(new Page<RefundRecordEntity>(1, 50, false),
                new LambdaQueryWrapper<RefundRecordEntity>().gt(RefundRecordEntity::getId, refundCursor)
                        .in(RefundRecordEntity::getRefundStatus, RefundStatusEnum.PROCESSING.getCode(), RefundStatusEnum.SUCCESS.getCode())
                        .le(RefundRecordEntity::getUpdatedAt, DateTimeUtil.nowUtc().minusSeconds(10))
                        .orderByAsc(RefundRecordEntity::getId)).getRecords();
        for (RefundRecordEntity refund : refunds) {
            refundCursor = refund.getId();
            try {
                if (RefundStatusEnum.PROCESSING.getCode().equals(refund.getRefundStatus())) {
                    orderService.refreshRefund(null, refund.getRefundNo());
                }
                orderService.reconcileRefundEffects(refund.getRefundNo());
            }
            catch (Exception e) { log.warn("退款补偿暂未完成，下轮重试: refundNo={}", refund.getRefundNo()); }
        }
        if (refunds.size() < 50) refundCursor = 0;
    }
}
