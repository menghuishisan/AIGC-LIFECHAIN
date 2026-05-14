package com.lifechain.app.config;

import com.lifechain.common.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.UUID;

/**
 * 全局响应增强：统一注入 traceId / requestId
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> apiResponse) {
            String traceId = UUID.randomUUID().toString().replace("-", "");
            String requestId = null;

            if (request instanceof ServletServerHttpRequest servletRequest) {
                HttpServletRequest httpRequest = servletRequest.getServletRequest();
                requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
            }

            if (apiResponse.getTraceId() == null) {
                apiResponse.setTraceId(traceId);
            }
            if (requestId != null && apiResponse.getRequestId() == null) {
                apiResponse.setRequestId(requestId);
            }

            response.getHeaders().set(TRACE_ID_HEADER, apiResponse.getTraceId());
            if (requestId != null) {
                response.getHeaders().set(REQUEST_ID_HEADER, requestId);
            }
        }
        return body;
    }
}
