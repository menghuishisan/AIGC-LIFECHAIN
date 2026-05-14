package com.lifechain.auth.audit;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogVO implements Serializable {
    private String targetType;
    private String targetNo;
    private String action;
    private String actionDetail;
    private String operatorRole;
    private String result;
    private String reasonCode;
    private LocalDateTime logTime;

    public static AuditLogVO fromEntity(AuditLogEntity entity) {
        if (entity == null) return null;
        return AuditLogVO.builder()
                .targetType(entity.getTargetType())
                .targetNo(entity.getTargetNo())
                .action(entity.getAction())
                .actionDetail(entity.getActionDetail())
                .operatorRole(entity.getOperatorRole())
                .result(entity.getResult())
                .reasonCode(entity.getReasonCode())
                .logTime(entity.getLogTime())
                .build();
    }
}
