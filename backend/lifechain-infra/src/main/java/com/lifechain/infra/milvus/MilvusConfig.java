package com.lifechain.infra.milvus;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 向量数据库配置
 * <p>
 * 读取 milvus 前缀的配置项，提供连接地址供 MilvusService 使用。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {

    private String host;
    private int port;
}
