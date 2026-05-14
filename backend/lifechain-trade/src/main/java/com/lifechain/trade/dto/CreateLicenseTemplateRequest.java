package com.lifechain.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLicenseTemplateRequest {
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    @NotBlank(message = "授权类型不能为空")
    private String licenseType;
    private String scopeDescription;
    private Integer durationDays;
    private Long priceAmount;
    private String currency;
    private String description;
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
