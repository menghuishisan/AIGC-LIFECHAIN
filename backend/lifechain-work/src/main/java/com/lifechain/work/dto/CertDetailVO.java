package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书详情视图对象（统一结构）
 */
@Data
public class CertDetailVO implements Serializable {

    private BasicInfo basicInfo;
    private StatusInfo statusInfo;
    private TimeInfo timeInfo;
    private RelationInfo relationInfo;
    private ChainInfo chainInfo;
    private List<String> allowedActions;

    @Data
    public static class BasicInfo implements Serializable {
        private String certNo;
        private String certHash;
        private String certFileUrl;
        private Integer version;
    }

    @Data
    public static class StatusInfo implements Serializable {
        private String status;
    }

    @Data
    public static class TimeInfo implements Serializable {
        private LocalDateTime issueTime;
        private LocalDateTime expireTime;
    }

    @Data
    public static class RelationInfo implements Serializable {
        private String workNo;
        private String claimNo;
    }

    @Data
    public static class ChainInfo implements Serializable {
        private String txHash;
        private Long blockHeight;
    }
}
