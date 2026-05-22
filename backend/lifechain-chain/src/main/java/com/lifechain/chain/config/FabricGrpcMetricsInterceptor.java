package com.lifechain.chain.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;

/**
 * Fabric gRPC 客户端调用指标采集
 * <p>
 * 为每次 fabric peer 的 gRPC 调用记录 timer：
 * <ul>
 *   <li>{@code fabric.grpc.client.duration} —— 调用耗时分布（method + status 两个 tag）</li>
 * </ul>
 * 通过 actuator/prometheus 端点暴露给采集系统。
 * </p>
 *
 * @author LifeChain
 */
@RequiredArgsConstructor
public class FabricGrpcMetricsInterceptor implements ClientInterceptor {

    private static final String METRIC_NAME = "fabric.grpc.client.duration";

    private final MeterRegistry meterRegistry;

    /**
     * 拦截 gRPC 客户端调用，统计 method 维度的 latency 与 status 分布
     *
     * @param method      gRPC 方法描述符
     * @param callOptions 调用选项
     * @param next        下游 Channel
     * @return            包装后的 ClientCall
     */
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        long startNanos = System.nanoTime();
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        long elapsed = System.nanoTime() - startNanos;
                        Timer.builder(METRIC_NAME)
                                .description("Hyperledger Fabric peer gRPC RPC duration")
                                .tags(Tags.of(
                                        "method", method.getFullMethodName(),
                                        "status", status.getCode().name()))
                                .register(meterRegistry)
                                .record(elapsed, TimeUnit.NANOSECONDS);
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }
}
