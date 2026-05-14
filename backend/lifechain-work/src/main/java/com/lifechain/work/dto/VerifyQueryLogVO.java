package com.lifechain.work.dto;

import com.lifechain.work.entity.VerifyQueryLogEntity;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class VerifyQueryLogVO implements Serializable {
    private String queryType;
    private String queryValue;
    private String querySource;
    private String queryIp;
    private Integer matchFound;
    private String resultSummary;
    private LocalDateTime queryTime;

    public static VerifyQueryLogVO fromEntity(VerifyQueryLogEntity entity) {
        if (entity == null) return null;
        return VerifyQueryLogVO.builder()
                .queryType(entity.getQueryType())
                .queryValue(entity.getQueryValue())
                .querySource(entity.getQuerySource())
                .queryIp(entity.getQueryIp())
                .matchFound(entity.getMatchFound())
                .resultSummary(entity.getResultSummary())
                .queryTime(entity.getQueryTime())
                .build();
    }
}
