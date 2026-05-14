package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 争议案件视图对象（统一结构）
 */
@Data
public class DisputeCaseVO implements Serializable {

    private BasicInfo basicInfo;
    private StatusInfo statusInfo;
    private TimeInfo timeInfo;
    private RelationInfo relationInfo;
    private ChainInfo chainInfo;
    private List<String> allowedActions;

    @Data
    public static class BasicInfo implements Serializable {
        private String caseNo;
        private String disputeType;
        private String description;
        private String resultSummary;
        private List<DisputeEvidenceVO> evidences;
        private List<ProcessRecordVO> processRecords;
    }

    @Data
    public static class StatusInfo implements Serializable {
        private String status;
    }

    @Data
    public static class TimeInfo implements Serializable {
        private LocalDateTime submitTime;
        private LocalDateTime acceptTime;
        private LocalDateTime closeTime;
        private LocalDateTime createdAt;
    }

    @Data
    public static class RelationInfo implements Serializable {
        private String orderNo;
        private String workNo;
        private String applicantAccountNo;
        private String respondentAccountNo;
    }

    @Data
    public static class ChainInfo implements Serializable {
        private String chainStatus;
        private String txHash;
        private Long blockHeight;
    }
}
