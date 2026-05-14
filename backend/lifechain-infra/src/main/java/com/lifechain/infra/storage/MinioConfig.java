package com.lifechain.infra.storage;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO对象存储配置
 * <p>
 * 读取 lifechain.minio 前缀的配置项，初始化 MinioClient 单例。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /** MinIO服务地址，例如 http://127.0.0.1:9000 */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 秘密密钥 */
    private String secretKey;

    /** 默认桶名 */
    private String bucketName;

    /**
     * 创建MinIO客户端实例
     *
     * @return MinioClient 单例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
