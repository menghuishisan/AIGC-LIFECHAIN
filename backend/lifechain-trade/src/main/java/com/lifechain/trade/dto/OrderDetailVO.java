package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情视图对象（统一结构）
 */
@Data
public class OrderDetailVO implements Serializable {

    private BasicInfo basicInfo;
    private StatusInfo statusInfo;
    private TimeInfo timeInfo;
    private RelationInfo relationInfo;
    private ChainInfo chainInfo;
    private List<String> allowedActions;

    @Data
    public static class BasicInfo implements Serializable {
        private String orderNo;
        private String workTitle;
        private String licenseType;
        private Long priceAmount;
        private Long payAmount;
        private String currency;
        private String payChannel;
    }

    @Data
    public static class StatusInfo implements Serializable {
        private String orderStatus;
        private String payStatus;
    }

    @Data
    public static class TimeInfo implements Serializable {
        private LocalDateTime createdAt;
        private LocalDateTime expireTime;
        private LocalDateTime payTime;
        private LocalDateTime completeTime;
    }

    @Data
    public static class RelationInfo implements Serializable {
        private String workNo;
        private String listingNo;
        private String buyerAccountNo;
        private String creatorAccountNo;
        private String licenseNo;
        private String settleNo;
    }

    @Data
    public static class ChainInfo implements Serializable {
        private String txHash;
        private Long blockHeight;
    }
}
