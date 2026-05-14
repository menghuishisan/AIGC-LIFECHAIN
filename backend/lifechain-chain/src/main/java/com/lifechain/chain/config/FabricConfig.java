package com.lifechain.chain.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.*;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Hyperledger Fabric 网关配置类
 * <p>
 * 读取 Fabric 网络连接参数，构建并管理 {@link Gateway} 和 {@link Network} 生命周期。
 * 配置前缀为 {@code lifechain.fabric}，对应 application.yml 中的区块链网络配置段。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "lifechain.fabric")
public class FabricConfig {

    /** 网络连接配置文件路径（connection-profile JSON/YAML） */
    private String networkConfigPath;

    /** 通道名称 */
    private String channelName;

    /** 组织 MSP ID */
    private String mspId;

    /** 用户标识 */
    private String userId;

    /** 用户证书 PEM 文件路径 */
    private String certPath;

    /** 用户私钥 PEM 文件路径 */
    private String keyPath;

    /** Peer 节点端点地址 */
    private String peerEndpoint;

    /** TLS 主机名覆盖（用于开发/测试环境） */
    private String overrideAuth;

    /** TLS CA 证书路径 */
    private String tlsCertPath;

    /** 网关实例引用，用于生命周期管理 */
    private Gateway gatewayInstance;

    /**
     * 创建并连接 Fabric Gateway
     * <p>
     * 从指定路径加载用户X509证书和私钥，构建内存钱包，
     * 使用网络连接配置文件初始化并连接 Gateway。
     * </p>
     *
     * @return 已连接的 Fabric Gateway 实例
     * @throws IOException          证书或配置文件读取失败
     * @throws CertificateException 证书解析失败
     */
    @Bean
    public Gateway gateway() throws IOException, CertificateException, java.security.InvalidKeyException {
        log.info("初始化 Fabric Gateway，MSP={}, 用户={}, 通道={}", mspId, userId, channelName);

        Path certFilePath = Paths.get(certPath);
        Path keyFilePath = Paths.get(keyPath);

        X509Certificate certificate;
        try (BufferedReader reader = Files.newBufferedReader(certFilePath)) {
            certificate = Identities.readX509Certificate(reader);
        }

        PrivateKey privateKey;
        try (BufferedReader reader = Files.newBufferedReader(keyFilePath)) {
            privateKey = Identities.readPrivateKey(reader);
        }

        Wallet wallet = Wallets.newInMemoryWallet();
        wallet.put(userId, Identities.newX509Identity(mspId, certificate, privateKey));
        log.info("用户身份已加载到内存钱包，证书主体={}", certificate.getSubjectX500Principal());

        Gateway.Builder builder = Gateway.createBuilder();
        builder.identity(wallet, userId);
        builder.networkConfig(Paths.get(networkConfigPath));
        builder.discovery(true);

        gatewayInstance = builder.connect();
        log.info("Fabric Gateway 连接成功");
        return gatewayInstance;
    }

    /**
     * 获取指定通道的 Network 实例
     *
     * @param gateway 已连接的 Fabric Gateway
     * @return 当前通道的 Network 实例
     */
    @Bean
    public Network network(Gateway gateway) {
        Network network = gateway.getNetwork(channelName);
        log.info("已获取通道网络，channelName={}", channelName);
        return network;
    }

    /**
     * 容器销毁时关闭 Gateway 连接，释放底层 gRPC 资源
     */
    @PreDestroy
    public void destroy() {
        if (gatewayInstance != null) {
            log.info("关闭 Fabric Gateway 连接");
            gatewayInstance.close();
        }
    }
}
