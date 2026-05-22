package com.lifechain.chain.config;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.Status;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 多 peer 静态列表 NameResolver
 * <p>
 * grpc-java 内置的 dns 解析器要求 DNS A 记录返回多 IP，开发与本地环境不可行；
 * 这里实现一个 scheme 为 {@code lifechain} 的解析器，把
 * {@code lifechain:///host1:port,host2:port,host3:port} 解析为多个等价地址，
 * 配合 channel 的 {@code defaultLoadBalancingPolicy("round_robin")} 实现客户端负载均衡，
 * 任何一个 peer 故障会被 grpc 健康检查标记为不可用、自动切换。
 * </p>
 *
 * @author LifeChain
 */
public class ListNameResolverProvider extends NameResolverProvider {

    /** target scheme，URI 形如 lifechain:///host1:port,host2:port */
    public static final String SCHEME = "lifechain";

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!SCHEME.equals(targetUri.getScheme())) {
            return null;
        }
        // URI 形如 lifechain:///host1:port,host2:port，path 部分以 "/" 开头
        String path = targetUri.getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            throw new IllegalArgumentException("空 peer 列表，URI=" + targetUri);
        }
        String hostList = path.substring(1);
        return new ListNameResolver(hostList);
    }

    /**
     * 静态列表解析器：构造时把所有 host:port 解析为 {@link EquivalentAddressGroup}，
     * 第一次 {@code start} 时一次性投递给 listener，无后续 refresh。
     */
    private static final class ListNameResolver extends NameResolver {

        private final String hostList;

        private ListNameResolver(String hostList) {
            this.hostList = hostList;
        }

        @Override
        public String getServiceAuthority() {
            // 取第一个 host 作为 authority，TLS overrideAuthority 通过 channel 单独配置覆盖
            return hostList.split(",", 2)[0];
        }

        @Override
        public void start(Listener2 listener) {
            try {
                List<EquivalentAddressGroup> groups = new ArrayList<>();
                for (String hp : hostList.split(",")) {
                    String trimmed = hp.trim();
                    if (trimmed.isEmpty()) continue;
                    int colon = trimmed.lastIndexOf(':');
                    if (colon <= 0) {
                        throw new IllegalArgumentException("非法 peer 端点（缺端口）: " + trimmed);
                    }
                    String host = trimmed.substring(0, colon);
                    int port = Integer.parseInt(trimmed.substring(colon + 1));
                    groups.add(new EquivalentAddressGroup(new InetSocketAddress(host, port)));
                }
                if (groups.isEmpty()) {
                    listener.onError(Status.UNAVAILABLE.withDescription("空 peer 列表"));
                    return;
                }
                listener.onResult(ResolutionResult.newBuilder()
                        .setAddresses(groups)
                        .setAttributes(Attributes.EMPTY)
                        .build());
            } catch (RuntimeException e) {
                listener.onError(Status.UNAVAILABLE.withCause(e).withDescription(e.getMessage()));
            }
        }

        @Override
        public void shutdown() {
            // 无连接需要释放
        }
    }

    /**
     * 把 host 列表拼成 lifechain scheme URI
     *
     * @param peers 逗号分隔或字符串列表的 peer 端点
     * @return      grpc forTarget 可用的 URI 字符串
     */
    public static String buildTarget(String peers) {
        return SCHEME + ":///" + String.join(",", Arrays.stream(peers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
    }
}
