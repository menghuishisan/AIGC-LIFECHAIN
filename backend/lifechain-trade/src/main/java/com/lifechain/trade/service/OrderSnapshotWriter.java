package com.lifechain.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifechain.common.util.HashUtil;
import com.lifechain.trade.entity.TradeOrderSnapshotEntity;
import com.lifechain.trade.mapper.TradeOrderSnapshotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 订单审计快照必须与订单处于同一事务，序列化或写库失败时交由上层回滚。 */
@Component
@RequiredArgsConstructor
public class OrderSnapshotWriter {

    private final ObjectMapper objectMapper;
    private final TradeOrderSnapshotMapper snapshotMapper;

    public void save(Long orderId, String orderNo, String snapshotType, Object data, LocalDateTime snapshotTime) {
        final String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("订单快照序列化失败: " + snapshotType, e);
        }

        TradeOrderSnapshotEntity snapshot = new TradeOrderSnapshotEntity();
        snapshot.setOrderId(orderId);
        snapshot.setOrderNo(orderNo);
        snapshot.setSnapshotType(snapshotType);
        snapshot.setSnapshotData(json);
        snapshot.setSnapshotHash(HashUtil.sha256(json));
        snapshot.setSnapshotTime(snapshotTime);
        if (snapshotMapper.insert(snapshot) != 1) {
            throw new IllegalStateException("订单快照写入失败: " + snapshotType);
        }
    }
}
