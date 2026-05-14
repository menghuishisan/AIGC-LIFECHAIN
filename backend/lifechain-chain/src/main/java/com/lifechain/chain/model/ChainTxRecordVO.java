package com.lifechain.chain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 链交易记录视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainTxRecordVO {
    private String bizType;
    private String bizNo;
    private String txType;
    private String channelName;
    private String chaincodeName;
    private String txHash;
    private Long blockHeight;
    private String chainStatus;
    private String endorsementSummary;
    private String failReason;
    private LocalDateTime submitTime;
    private LocalDateTime confirmTime;
    private LocalDateTime createdAt;
}
