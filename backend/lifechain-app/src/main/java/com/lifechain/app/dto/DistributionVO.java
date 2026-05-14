package com.lifechain.app.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
public class DistributionVO implements Serializable {
    private Map<String, Long> worksByStatus;
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> accountsByType;
    private Map<String, Long> riskByLevel;
}
