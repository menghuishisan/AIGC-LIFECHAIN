package com.lifechain.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/** 为支付后的授权启动设置有界线程池，避免远端链调用占用回调线程。 */
@Configuration
public class LicenseGrantAsyncConfiguration {

    @Bean("licenseGrantExecutor")
    public ThreadPoolTaskExecutor licenseGrantExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("license-grant-");
        // 饱和时拒绝即时任务，由持久订单扫描恢复；禁止退回支付回调线程执行链调用。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
