package com.lifechain.common.exception;

import com.lifechain.common.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 业务异常基类
 * <p>
 * 所有业务异常统一挂标准错误码，由全局异常处理器统一捕获并返回标准响应。
 * </p>
 */
@Getter
public class BizException extends RuntimeException {

    /** 错误码 */
    private final ErrorCodeEnum errorCode;

    /** 原因码（可选，用于更细粒度的错误分类） */
    private final String reasonCode;

    /** 当前状态（可选，状态校验失败时使用） */
    private final String currentStatus;

    public BizException(ErrorCodeEnum errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.reasonCode = null;
        this.currentStatus = null;
    }

    public BizException(ErrorCodeEnum errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.reasonCode = null;
        this.currentStatus = null;
    }

    public BizException(ErrorCodeEnum errorCode, String message, String reasonCode) {
        super(message);
        this.errorCode = errorCode;
        this.reasonCode = reasonCode;
        this.currentStatus = null;
    }

    public BizException(ErrorCodeEnum errorCode, String message, String reasonCode, String currentStatus) {
        super(message);
        this.errorCode = errorCode;
        this.reasonCode = reasonCode;
        this.currentStatus = currentStatus;
    }

    public BizException(ErrorCodeEnum errorCode, Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.errorCode = errorCode;
        this.reasonCode = null;
        this.currentStatus = null;
    }

    public BizException(ErrorCodeEnum errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.reasonCode = null;
        this.currentStatus = null;
    }
}
