package com.lifechain.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lifechain.common.enums.ErrorCodeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 统一API响应结构
 * <p>
 * 所有接口统一返回此对象，包含业务码、消息、追踪ID、请求ID和数据体。
 * 失败时额外返回 reasonCode、currentStatus、allowedActions、fieldErrors。
 * </p>
 *
 * @param <T> 数据体类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    /** 业务码，000000表示成功 */
    private String code;

    /** 响应消息 */
    private String message;

    /** 是否成功 */
    private boolean success;

    /** 链路追踪ID */
    private String traceId;

    /** 请求幂等ID */
    private String requestId;

    /** 响应数据 */
    private T data;

    /** 失败时的原因码 */
    private String reasonCode;

    /** 失败时的当前状态 */
    private String currentStatus;

    /** 失败时可执行的动作列表 */
    private List<String> allowedActions;

    /** 字段校验错误 */
    private Map<String, String> fieldErrors;

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(ErrorCodeEnum.SUCCESS.getCode());
        response.setMessage(ErrorCodeEnum.SUCCESS.getDescription());
        response.setSuccess(true);
        return response;
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = success();
        response.setData(data);
        return response;
    }

    /**
     * 失败响应
     *
     * @param errorCode 错误码枚举
     */
    public static <T> ApiResponse<T> fail(ErrorCodeEnum errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(errorCode.getCode());
        response.setMessage(errorCode.getDescription());
        response.setSuccess(false);
        return response;
    }

    /**
     * 失败响应（自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义消息
     */
    public static <T> ApiResponse<T> fail(ErrorCodeEnum errorCode, String message) {
        ApiResponse<T> response = fail(errorCode);
        response.setMessage(message);
        return response;
    }

    /**
     * 设置追踪ID
     */
    public ApiResponse<T> traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * 设置请求ID
     */
    public ApiResponse<T> requestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * 设置原因码
     */
    public ApiResponse<T> reasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
        return this;
    }

    /**
     * 设置当前状态
     */
    public ApiResponse<T> currentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
        return this;
    }

    /**
     * 设置可执行动作
     */
    public ApiResponse<T> allowedActions(List<String> allowedActions) {
        this.allowedActions = allowedActions;
        return this;
    }

    /**
     * 设置字段错误
     */
    public ApiResponse<T> fieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
        return this;
    }
}
