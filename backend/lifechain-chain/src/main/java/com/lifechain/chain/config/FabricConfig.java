package com.lifechain.chain.config;

import io.grpc.ManagedChannel;
import io.grpc.NameResolverRegistry;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hyperledger Fabric 网关配置类（基于新版 fabric-gateway SDK 1.7，生产级加固）
 * <p>
 * 配置前缀 {@code lifechain.fabric}，对应 application.yml 中的区块链网络配置段。
 * 加固项：
 * <ol>
 *   <li>mTLS 双向认证（peer CLIENTAUTHREQUIRED=true，客户端必须出示 TLS 客户端证书）</li>
 *   <li>多 peer 列表 + round_robin LB（自定义 ListNameResolverProvider）</li>
 *   <li>gRPC keepalive（防 NAT/LB 静默断开长连接）</li>
 *   <li>所有 4 类 RPC（evaluate/endorse/submit/commitStatus）显式 deadline</li>
 *   <li>仅对 evaluate（幂等）启用 retry policy；submit 路径靠 chain_tx_record 去重</li>
 *   <li>Netty maxInboundMessageSize 提升 + 显式工作线程</li>
 *   <li>Micrometer ClientInterceptor 暴露 fabric.grpc.client.duration 指标</li>
 *   <li>Gateway/Channel 由 {@link FabricGatewayHolder} 管理生命周期，支持 TLS 证书热重载</li>
 *   <li>优雅关闭：channel.shutdown() + awaitTermination 在 holder 内统一处理</li>
 * </ol>
 * 本类只暴露配置属性与单例 Bean；具体的连接 / 重建逻辑都在 {@link FabricGatewayHolder} 中。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "lifechain.fabric")
public class FabricConfig {

    static {
        // 注册自定义 NameResolverProvider，让 NettyChannelBuilder.forTarget("lifechain:///host1,host2") 可解析
        NameResolverRegistry.getDefaultRegistry().register(new ListNameResolverProvider());
    }

    /** 通道名称 */
    private String channelName;

    /** 组织 MSP ID */
    private String mspId;

    /** 用户 MSP X509 证书 PEM 文件路径（应用层身份） */
    private String certPath;

    /** 用户 MSP 私钥 PEM 文件路径（应用层身份） */
    private String keyPath;

    /** Peer 节点 gRPC 端点列表（逗号分隔，自动 round_robin LB） */
    private String peerEndpoints;

    /** Peer 服务端 TLS CA 证书 PEM 文件路径（校验 peer 服务端证书） */
    private String tlsCertPath;

    /** mTLS 客户端 TLS 证书 PEM 路径（peer 启用 CLIENTAUTHREQUIRED 时必填） */
    private String tlsClientCertPath;

    /** mTLS 客户端 TLS 私钥 PEM 路径 */
    private String tlsClientKeyPath;

    /** TLS SNI / authority 覆盖（开发/测试环境匹配 peer 证书 CN） */
    private String overrideAuth;

    /** evaluate（链上查询）超时秒数 */
    private int timeoutEvaluate = 5;

    /** endorse（背书收集）超时秒数 */
    private int timeoutEndorse = 15;

    /** submit（提交到 orderer）超时秒数 */
    private int timeoutSubmit = 5;

    /** commit status（等待 orderer 出块）超时秒数 */
    private int timeoutCommit = 60;

    /**
     * 构建到 peer 的 gRPC 通道：mTLS + keepalive + round_robin LB + retry + metrics
     *
     * @param meterRegistry Micrometer 注册中心，用于挂 RPC 指标拦截器
     * @return              建立完成的 ManagedChannel
     * @throws IOException  TLS 证书或私钥读取失败
     */
    public ManagedChannel buildChannel(MeterRegistry meterRegistry) throws IOException {
        log.info("初始化 Fabric gRPC channel: peers={}, overrideAuth={}", peerEndpoints, overrideAuth);

        File peerCa = Paths.get(tlsCertPath).toFile();
        File clientCert = Paths.get(tlsClientCertPath).toFile();
        File clientKey = Paths.get(tlsClientKeyPath).toFile();
        SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(peerCa)
                .keyManager(clientCert, clientKey)
                .build();

        NettyChannelBuilder builder = NettyChannelBuilder
                .forTarget(ListNameResolverProvider.buildTarget(peerEndpoints))
                .sslContext(sslContext)
                // 多 peer round_robin LB，配合 ListNameResolverProvider 解析的多地址生效
                .defaultLoadBalancingPolicy("round_robin")
                // 长连接 keepalive：60s 没流量就发 ping，20s 内必须有响应
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(20, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                // 提升入站消息上限（链码返回大对象不被截断）+ HTTP/2 流控窗口
                .maxInboundMessageSize(50 * 1024 * 1024)
                .flowControlWindow(8 * 1024 * 1024)
                // 仅对幂等的 evaluate 类调用启用重试
                .enableRetry()
                .defaultServiceConfig(buildRetryServiceConfig())
                // 注入 Micrometer 指标拦截器
                .intercept(new FabricGrpcMetricsInterceptor(meterRegistry));

        if (overrideAuth != null && !overrideAuth.isBlank()) {
            builder.overrideAuthority(overrideAuth);
        }

        ManagedChannel channel = builder.build();
        log.info("Fabric gRPC channel 已建立");
        return channel;
    }

    /**
     * 基于已建立的 gRPC channel 创建 Gateway，并显式设置四类 RPC 的 deadline
     *
     * @param channel 已建立的 ManagedChannel
     * @return        已连接的 Gateway 实例
     * @throws IOException                       MSP 证书 / 私钥读取失败
     * @throws CertificateException              证书解析失败
     * @throws java.security.InvalidKeyException 私钥解析失败
     */
    public Gateway buildGateway(ManagedChannel channel)
            throws IOException, CertificateException, java.security.InvalidKeyException {
        log.info("初始化 Fabric Gateway: MSP={}, channel={}", mspId, channelName);

        Path certFilePath = Paths.get(certPath);
        Path keyFilePath = Paths.get(keyPath);

        X509Certificate certificate;
        try (BufferedReader reader = Files.newBufferedReader(certFilePath)) {
            certificate = Identities.readX509Certificate(reader);
        }
        Identity identity = new X509Identity(mspId, certificate);

        PrivateKey privateKey;
        try (BufferedReader reader = Files.newBufferedReader(keyFilePath)) {
            privateKey = Identities.readPrivateKey(reader);
        }
        Signer signer = Signers.newPrivateKeySigner(privateKey);

        Gateway gateway = Gateway.newInstance()
                .identity(identity)
                .signer(signer)
                .connection(channel)
                // 新版 SDK 用 UnaryOperator<CallOptions> 替代 deprecated 的 CallOption... 数组
                .evaluateOptions(opts -> opts.withDeadlineAfter(timeoutEvaluate, TimeUnit.SECONDS))
                .endorseOptions(opts -> opts.withDeadlineAfter(timeoutEndorse, TimeUnit.SECONDS))
                .submitOptions(opts -> opts.withDeadlineAfter(timeoutSubmit, TimeUnit.SECONDS))
                .commitStatusOptions(opts -> opts.withDeadlineAfter(timeoutCommit, TimeUnit.SECONDS))
                .connect();
        log.info("Fabric Gateway 连接成功");
        return gateway;
    }

    /**
     * 构建 gRPC retry service config（JSON 形式）
     * <p>
     * 仅对 fabric peer EvaluateService（链上查询，幂等）启用重试，
     * 5 次内指数回退；endorse / submit / commit_status 因为不幂等，不能盲重试，
     * 由业务层通过 chain_tx_record + 业务幂等键控制。
     * </p>
     *
     * @return service config map（gRPC 期望的格式）
     */
    private Map<String, Object> buildRetryServiceConfig() {
        Map<String, Object> retryPolicy = Map.of(
                "maxAttempts", 5.0d,
                "initialBackoff", "0.5s",
                "maxBackoff", "5s",
                "backoffMultiplier", 2.0d,
                "retryableStatusCodes", java.util.List.of("UNAVAILABLE", "DEADLINE_EXCEEDED")
        );
        return Map.of(
                "methodConfig", java.util.List.of(Map.of(
                        "name", java.util.List.of(Map.of(
                                "service", "gateway.Gateway",
                                "method", "Evaluate"
                        )),
                        "retryPolicy", retryPolicy
                ))
        );
    }

    /**
     * 暴露给 Spring 的单例：fabric 网关持有者
     * <p>
     * 业务通过它拿当前 Network；它内部维护 channel/gateway 生命周期，
     * 支持 TLS 证书热重载。
     * </p>
     *
     * @param meterRegistry Micrometer 注册中心
     * @return              FabricGatewayHolder
     */
    @Bean
    public FabricGatewayHolder fabricGatewayHolder(MeterRegistry meterRegistry) {
        return new FabricGatewayHolder(this, meterRegistry);
    }
}
