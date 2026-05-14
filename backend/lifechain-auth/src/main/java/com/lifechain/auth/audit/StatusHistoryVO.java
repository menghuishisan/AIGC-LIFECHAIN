package com.lifechain.auth.audit;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class StatusHistoryVO implements Serializable {
    private String bizType;
    private String bizNo;
    private String fromStatus;
    private String toStatus;
    private String changeReason;
    private String reasonCode;
    private LocalDateTime changeTime;

    public static StatusHistoryVO fromEntity(StatusHistoryEntity entity) {
        if (entity == null) return null;
        return StatusHistoryVO.builder()
                .bizType(entity.getBizType())
                .bizNo(entity.getBizNo())
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .changeReason(entity.getChangeReason())
                .reasonCode(entity.getReasonCode())
                .changeTime(entity.getChangeTime())
                .build();
    }
}
