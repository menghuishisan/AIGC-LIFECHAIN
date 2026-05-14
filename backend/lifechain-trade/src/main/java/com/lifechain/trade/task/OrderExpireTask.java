package com.lifechain.trade.task;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.OrderStatusEnum;
import com.lifechain.common.enums.PayStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.trade.entity.PaymentRecordEntity;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.PaymentRecordMapper;
import com.lifechain.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单过期自动取消定时任务
 * <p>
 * 每60秒扫描一次状态为 ORDER_CREATED 且已超过过期时间的订单，
 * 自动将其状态变更为 ORDER_EXPIRED，并关闭关联的待支付记录。
 * 所有状态变更均记录到状态变更历史。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpireTask {

    private final TradeOrderMapper orderMapper;
    private final PaymentRecordMapper paymentMapper;
    private final AuditService auditService;

    /**
     * 扫描并处理过期订单
     * <p>
     * 固定延迟60秒执行一次，查询所有已过期未支付的订单，
     * 逐笔更新状态并关闭支付记录，写入状态变更历史。
     * </p>
     */
    @Scheduled(fixedDelay = 60000)
    public void expireOrders() {
        List<TradeOrderEntity> expiredOrders = orderMapper.selectExpiredOrders();
        if (expiredOrders.isEmpty()) {
            return;
        }
        log.info("扫描到{}笔过期订单，开始处理", expiredOrders.size());

        for (TradeOrderEntity order : expiredOrders) {
            try {
                processExpiredOrder(order);
            } catch (Exception e) {
                log.error("过期订单处理失败: orderNo={}", order.getOrderNo(), e);
            }
        }

        log.info("过期订单处理完成，共处理{}笔", expiredOrders.size());
    }

    /**
     * 处理单笔过期订单
     * <p>
     * 将订单状态从 ORDER_CREATED 变更为 ORDER_EXPIRED，
     * 关闭关联的待支付记录，记录状态变更历史。
     * </p>
     *
     * @param order 过期订单实体
     */
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredOrder(TradeOrderEntity order) {
        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatusEnum.ORDER_EXPIRED.getCode());
        order.setCancelTime(DateTimeUtil.nowUtc());
        order.setCancelReason("订单超时自动过期");
        orderMapper.updateById(order);

        // 关闭待支付的支付记录
        List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
        for (PaymentRecordEntity payment : payments) {
            if (PayStatusEnum.PAY_PENDING.getCode().equals(payment.getPayStatus())
                    || PayStatusEnum.PAY_INIT.getCode().equals(payment.getPayStatus())) {
                payment.setPayStatus(PayStatusEnum.PAY_CLOSED.getCode());
                paymentMapper.updateById(payment);
            }
        }

        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                fromStatus, OrderStatusEnum.ORDER_EXPIRED.getCode(),
                "订单超时自动过期", "ORDER_EXPIRED", null);

        log.info("过期订单已处理: orderNo={}", order.getOrderNo());
    }
}
