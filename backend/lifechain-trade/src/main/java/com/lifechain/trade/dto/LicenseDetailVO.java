package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权详情视图对象（统一结构）
 */
@Data
public class LicenseDetailVO implements Serializable {

    private BasicInfo basicInfo;
    private StatusInfo statusInfo;
    private TimeInfo timeInfo;
    private RelationInfo relationInfo;
    private ChainInfo chainInfo;
    private List<String> allowedActions;

    @Data
    public static class BasicInfo implements Serializable {
        private String licenseNo;
        private String licenseType;
        private String scopeDescription;
    }

    @Data
    public static class StatusInfo implements Serializable {
        private String licenseStatus;
    }

    @Data
    public static class TimeInfo implements Serializable {
        private LocalDateTime effectiveTime;
        private LocalDateTime expireTime;
    }

    @Data
    public static class RelationInfo implements Serializable {
        private String orderNo;
        private String workNo;
    }

    @Data
    public static class ChainInfo implements Serializable {
        private String chainStatus;
        private String txHash;
        private Long blockHeight;
        private String licenseHash;
    }
}
