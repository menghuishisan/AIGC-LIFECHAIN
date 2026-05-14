package com.lifechain.trade.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class LicenseTemplateVO implements Serializable {
    private String templateName;
    private String templateCode;
    private String licenseType;
    private String scopeDescription;
    private Integer durationDays;
    private Long priceAmount;
    private String currency;
    private String status;
    private String description;
    private LocalDateTime createdAt;
}
