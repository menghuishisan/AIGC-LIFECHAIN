package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 确权申请详情视图对象（统一结构）
 */
@Data
public class ClaimDetailVO implements Serializable {

    private BasicInfo basicInfo;
    private StatusInfo statusInfo;
    private TimeInfo timeInfo;
    private RelationInfo relationInfo;
    private ChainInfo chainInfo;
    private List<String> allowedActions;

    @Data
    public static class BasicInfo implements Serializable {
        private String claimNo;
        private String summaryHash;
    }

    @Data
    public static class StatusInfo implements Serializable {
        private String status;
        private String reviewComment;
        private String rejectReason;
    }

    @Data
    public static class TimeInfo implements Serializable {
        private LocalDateTime submitTime;
        private LocalDateTime reviewTime;
        private LocalDateTime approveTime;
    }

    @Data
    public static class RelationInfo implements Serializable {
        private String workNo;
    }

    @Data
    public static class ChainInfo implements Serializable {
        private String chainStatus;
        private String txHash;
        private Long blockHeight;
    }
}
