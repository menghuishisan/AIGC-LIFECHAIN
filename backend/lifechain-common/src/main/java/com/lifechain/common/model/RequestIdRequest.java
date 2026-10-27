package com.lifechain.common.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 无其他业务字段的写操作请求，复用统一幂等组件。 */
@Data
public class RequestIdRequest {
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
