package com.lifechain.trade.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/** 支付事务提交后再异步启动授权，避免在支付回调事务中等待 Fabric。 */
@Slf4j
@Component
public class LicenseGrantEventListener {

    private final OrderService orderService;
    private final Executor executor;

    public LicenseGrantEventListener(OrderService orderService,
                                    @Qualifier("licenseGrantExecutor") Executor executor) {
        this.orderService = orderService;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(LicenseGrantRequestedEvent event) {
        try {
            executor.execute(() -> {
                try {
                    orderService.startLicenseGrant(event.orderNo());
                } catch (Exception e) {
                    log.error("支付后授权启动失败，将由定时补偿恢复: orderNo={}", event.orderNo(), e);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("授权执行队列已满，将由定时补偿恢复: orderNo={}", event.orderNo());
        }
    }
}
