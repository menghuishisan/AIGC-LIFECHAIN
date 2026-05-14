package com.lifechain.app.service;

import com.lifechain.app.assembler.TraceVoAssembler;
import com.lifechain.app.dto.TraceEventVO;
import com.lifechain.app.entity.TraceEventEntity;
import com.lifechain.app.mapper.TraceEventMapper;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TraceQueryService {

    private final TraceEventMapper traceEventMapper;
    private final WorkMapper workMapper;
    private final TradeOrderMapper tradeOrderMapper;

    public WorkEntity getWorkByNo(String workNo) {
        return workMapper.selectByWorkNo(workNo);
    }

    public TradeOrderEntity getOrderByNo(String orderNo) {
        return tradeOrderMapper.selectByOrderNo(orderNo);
    }

    public List<TraceEventVO> queryTraceEvents(String bizType, String bizNo) {
        List<TraceEventEntity> entities = traceEventMapper.selectByBizNo(bizType, bizNo);
        boolean showExtra = TraceVoAssembler.isExtraDataVisible();
        return entities.stream()
                .map(e -> TraceEventVO.builder()
                        .eventType(e.getEventType())
                        .eventTime(e.getEventTime())
                        .description(e.getEventDescription())
                        .operator(e.getOperatorRole())
                        .extraData(showExtra ? e.getExtraData() : null)
                        .build())
                .toList();
    }
}
