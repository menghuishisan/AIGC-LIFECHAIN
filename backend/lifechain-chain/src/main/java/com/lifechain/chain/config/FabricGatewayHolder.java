package com.lifechain.chain.config;

import io.grpc.ManagedChannel;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Network;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Fabric Gateway / Channel 单例持有者
 * <p>
 * 设计目的：
 * <ul>
 *   <li>把 channel + gateway 的生命周期封装在一起，业务方通过 {@link #getNetwork()}
 *       拿当前活跃的 Network，无需关心底层连接细节。</li>
 *   <li>启动时从 {@link FabricConfig} 配置参数构建 channel + gateway。</li>
 *   <li>启动一个 daemon 线程，监听 {@code runtime-config/fabric/org1/} 目录下的所有
 *       PEM 文件变更（MSP 证书 / TLS 证书 / mTLS 客户端证书），变更时加锁重建
 *       channel + gateway，期间业务调用阻塞等待，重建完成自动放行。</li>
 *   <li>容器停机时优雅关闭：先 close gateway 让 in-flight 调用完成 →
 *       channel.shutdown() → awaitTermination(30s) → 超时再 shutdownNow()。</li>
 * </ul>
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
public class FabricGatewayHolder {

    /** 优雅关闭等待时长（秒） */
    private static final int GRACEFUL_SHUTDOWN_SECONDS = 30;

    /** 证书变更检测的去抖时长（毫秒）：避免编辑器多次保存触发多次重建 */
    private static final long DEBOUNCE_MS = 1_500L;

    private final FabricConfig config;
    private final MeterRegistry meterRegistry;
    private final ReentrantLock rebuildLock = new ReentrantLock();

    private volatile ManagedChannel channel;
    private volatile Gateway gateway;
    private volatile Thread watchThread;
    private volatile boolean shuttingDown;

    public FabricGatewayHolder(FabricConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 启动时初始化 channel + gateway，并启动证书监听线程
     */
    @PostConstruct
    public void init() {
        rebuild("init");
        startCertificateWatcher();
    }

    /**
     * 获取当前 channel 上的 Network
     * <p>
     * 期间若正在重建（持有重建锁），调用方会阻塞，直到新 gateway 就绪。
     * </p>
     *
     * @return 当前活跃 Network
     */
    public Network getNetwork() {
        rebuildLock.lock();
        try {
            return gateway.getNetwork(config.getChannelName());
        } finally {
            rebuildLock.unlock();
        }
    }

    /**
     * 加锁重建 channel + gateway；调用前会先优雅关闭旧实例
     *
     * @param reason 触发原因（用于日志）
     */
    public void rebuild(String reason) {
        rebuildLock.lock();
        try {
            log.info("重建 Fabric Gateway，原因={}", reason);
            closeQuietly();
            try {
                ManagedChannel newChannel = config.buildChannel(meterRegistry);
                Gateway newGateway = config.buildGateway(newChannel);
                this.channel = newChannel;
                this.gateway = newGateway;
                log.info("Fabric Gateway 重建完成");
            } catch (IOException | CertificateException | java.security.InvalidKeyException e) {
                throw new IllegalStateException("Fabric Gateway 构建失败: " + e.getMessage(), e);
            }
        } finally {
            rebuildLock.unlock();
        }
    }

    /**
     * 启动一个 daemon 线程，监听 runtime-config/fabric/org1 目录下 *.pem 变化，
     * 变更后去抖 1.5 秒再重建（避免编辑器多次保存触发多次重建）
     */
    private void startCertificateWatcher() {
        Path watchDir = inferWatchDir();
        if (watchDir == null) {
            log.warn("无法推断 fabric 证书所在目录，TLS 热重载已禁用");
            return;
        }

        watchThread = new Thread(() -> runWatchLoop(watchDir), "fabric-cert-watch");
        watchThread.setDaemon(true);
        watchThread.start();
        log.info("Fabric 证书热重载监听已启动: {}", watchDir);
    }

    /**
     * 推断要监听的目录：取 cert/key/tlsCert/clientCert/clientKey 文件的公共父目录
     *
     * @return 监听目录；推断不出返回 null
     */
    private Path inferWatchDir() {
        Path[] files = new Path[]{
                Paths.get(config.getCertPath()).toAbsolutePath(),
                Paths.get(config.getKeyPath()).toAbsolutePath(),
                Paths.get(config.getTlsCertPath()).toAbsolutePath(),
                Paths.get(config.getTlsClientCertPath()).toAbsolutePath(),
                Paths.get(config.getTlsClientKeyPath()).toAbsolutePath()
        };
        Path parent = files[0].getParent();
        for (Path p : files) {
            if (parent == null || !p.startsWith(parent)) {
                // 文件分散在不同目录时，简化处理：只监听第一个文件的父目录
                return files[0].getParent();
            }
        }
        return parent;
    }

    /**
     * 监听循环：捕获 PEM 文件 modify/create/delete 事件，去抖后触发 rebuild
     *
     * @param watchDir 要监听的目录
     */
    private void runWatchLoop(Path watchDir) {
        Set<String> watchedFileNames = new HashSet<>();
        for (String p : new String[]{config.getCertPath(), config.getKeyPath(),
                config.getTlsCertPath(), config.getTlsClientCertPath(), config.getTlsClientKeyPath()}) {
            watchedFileNames.add(Paths.get(p).getFileName().toString());
        }

        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            watchDir.register(ws,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE);

            while (!shuttingDown && !Thread.currentThread().isInterrupted()) {
                WatchKey key = ws.poll(1, TimeUnit.SECONDS);
                if (key == null) continue;

                boolean shouldRebuild = false;
                for (WatchEvent<?> ev : key.pollEvents()) {
                    Object ctx = ev.context();
                    if (ctx instanceof Path p && watchedFileNames.contains(p.getFileName().toString())) {
                        shouldRebuild = true;
                    }
                }
                if (!key.reset()) {
                    log.warn("证书监听 key 失效，停止监听");
                    return;
                }
                if (!shouldRebuild) continue;

                // 去抖：吃掉 1.5s 内的连续事件
                Thread.sleep(DEBOUNCE_MS);
                WatchKey extra;
                while ((extra = ws.poll()) != null) {
                    extra.pollEvents();
                    extra.reset();
                }

                try {
                    rebuild("certificate change detected");
                } catch (RuntimeException e) {
                    log.error("证书变更后重建 Fabric Gateway 失败，旧连接保留", e);
                }
            }
        } catch (IOException e) {
            log.error("Fabric 证书监听异常", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 容器销毁时优雅关闭：先 close gateway → channel.shutdown() →
     * awaitTermination(30s) → 超时再 shutdownNow()。
     */
    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        if (watchThread != null) {
            watchThread.interrupt();
        }
        closeQuietly();
    }

    /**
     * 关闭当前 gateway + channel，吞掉异常以保证调用方安全
     */
    private void closeQuietly() {
        if (gateway != null) {
            try {
                gateway.close();
            } catch (RuntimeException e) {
                log.warn("关闭 Fabric Gateway 异常", e);
            }
            gateway = null;
        }
        if (channel != null) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(GRACEFUL_SHUTDOWN_SECONDS, TimeUnit.SECONDS)) {
                    log.warn("Fabric channel 优雅关闭超时（{}s），强制关闭", GRACEFUL_SHUTDOWN_SECONDS);
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                channel.shutdownNow();
            }
            channel = null;
        }
    }
}
