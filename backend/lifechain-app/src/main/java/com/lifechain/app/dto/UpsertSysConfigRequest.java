package com.lifechain.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpsertSysConfigRequest {

    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @NotBlank(message = "配置值不能为空")
    private String configValue;

    private String configType;

    private String description;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
