package com.lifechain.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AIGC内容可信管理平台 - 启动类
 * <p>
 * 扫描所有 com.lifechain 包下的组件，启用定时任务调度，
 * 自动注册所有模块的 MyBatis Mapper 接口。
 * </p>
 *
 * @author LifeChain
 */
@SpringBootApplication(scanBasePackages = "com.lifechain")
@MapperScan("com.lifechain")
@EnableScheduling
public class LifeChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeChainApplication.class, args);
    }
}
