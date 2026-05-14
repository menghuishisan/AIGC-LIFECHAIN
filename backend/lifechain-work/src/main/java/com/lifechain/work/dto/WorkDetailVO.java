package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品详情视图对象（统一结构）
 */
@Data
public class WorkDetailVO implements Serializable {

    private BasicInfo basicInfo;
    private StatusInfo statusInfo;
    private TimeInfo timeInfo;
    private RelationInfo relationInfo;
    private ChainInfo chainInfo;
    private List<String> allowedActions;

    @Data
    public static class BasicInfo implements Serializable {
        private String workNo;
        private String title;
        private String description;
        private String workType;
        private String coverUrl;
        private List<WorkFileVO> files;
        private AigcMetaDTO aigcMeta;
        private WorkFeatureVO feature;
    }

    @Data
    public static class StatusInfo implements Serializable {
        private String status;
    }

    @Data
    public static class TimeInfo implements Serializable {
        private LocalDateTime createdAt;
        private LocalDateTime submitTime;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class RelationInfo implements Serializable {
        private String claimNo;
        private String certNo;
        private String listingNo;
    }

    @Data
    public static class ChainInfo implements Serializable {
        private String txHash;
        private Long blockHeight;
    }
}
