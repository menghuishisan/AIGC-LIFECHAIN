package com.lifechain.app.service;

import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 恢复支付提交后进程退出或异步任务未启动的授权，复用订单服务的同一幂等入口。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseGrantReconciliationJob {

    private final TradeOrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${lifechain.trade.license-grant-reconcile-delay-ms:10000}", initialDelay = 10000)
    public void reconcile() {
        for (var order : orderMapper.selectPaidOrdersPendingLicenseGrant(50)) {
            try {
                orderService.startLicenseGrant(order.getOrderNo());
            } catch (Exception e) {
                log.warn("订单授权待补偿: orderNo={}", order.getOrderNo(), e);
            }
        }
    }
}
