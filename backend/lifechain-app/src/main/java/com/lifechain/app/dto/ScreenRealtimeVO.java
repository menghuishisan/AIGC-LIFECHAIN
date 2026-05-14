package com.lifechain.app.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ScreenRealtimeVO implements Serializable {
    private Long totalAccounts;
    private Long totalWorks;
    private Long totalOrders;
    private Long totalSettlements;
    private Long totalClaims;
    private Long totalRiskEvents;
    private Long totalFreezeRecords;
    private Long todayNewWorks;
    private Long todayNewOrders;
    private Long todayNewClaims;
}
