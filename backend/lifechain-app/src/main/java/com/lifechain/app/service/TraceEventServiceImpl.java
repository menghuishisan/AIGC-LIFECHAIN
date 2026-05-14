package com.lifechain.app.service;

import com.lifechain.app.entity.TraceEventEntity;
import com.lifechain.app.mapper.TraceEventMapper;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.common.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 统一溯源事件服务实现
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceEventServiceImpl implements TraceEventService {

    private final TraceEventMapper traceEventMapper;

    @Override
    public void writeTraceEvent(String bizType, Long bizId, String bizNo,
                                String eventType, String eventDescription,
                                Long operatorId, String operatorRole, String extraData) {
        TraceEventEntity entity = TraceEventEntity.builder()
                .bizType(bizType)
                .bizId(bizId)
                .bizNo(bizNo)
                .eventType(eventType)
                .eventDescription(eventDescription)
                .operatorId(operatorId)
                .operatorRole(operatorRole)
                .eventTime(DateTimeUtil.nowUtc())
                .extraData(extraData)
                .build();
        traceEventMapper.insert(entity);
        log.info("溯源事件已写入，bizType={}, bizNo={}, eventType={}", bizType, bizNo, eventType);
    }
}
