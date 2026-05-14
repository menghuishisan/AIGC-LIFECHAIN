package com.lifechain.app.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class RegulatorSearchResultVO implements Serializable {
    private String objectType;
    private String objectNo;
    private String title;
    private String status;
    private String matchField;
    private LocalDateTime createdAt;
}
