package com.lifechain.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifechain.app.dto.ScreenRealtimeVO;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.regulator.mapper.FreezeRecordMapper;
import com.lifechain.regulator.mapper.RiskEventMapper;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.work.entity.ClaimApplicationEntity;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.ClaimApplicationMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScreenService {

    private final AccountMapper accountMapper;
    private final WorkMapper workMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final ClaimApplicationMapper claimApplicationMapper;
    private final RiskEventMapper riskEventMapper;
    private final FreezeRecordMapper freezeRecordMapper;

    public ScreenRealtimeVO getRealtimeData() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        return ScreenRealtimeVO.builder()
                .totalAccounts(accountMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalWorks(workMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalOrders(tradeOrderMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalSettlements(settlementRecordMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalClaims(claimApplicationMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalRiskEvents(riskEventMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalFreezeRecords(freezeRecordMapper.selectCount(new LambdaQueryWrapper<>()))
                .todayNewWorks(workMapper.selectCount(new LambdaQueryWrapper<WorkEntity>()
                        .ge(WorkEntity::getCreatedAt, todayStart)))
                .todayNewOrders(tradeOrderMapper.selectCount(new LambdaQueryWrapper<TradeOrderEntity>()
                        .ge(TradeOrderEntity::getCreatedAt, todayStart)))
                .todayNewClaims(claimApplicationMapper.selectCount(new LambdaQueryWrapper<ClaimApplicationEntity>()
                        .ge(ClaimApplicationEntity::getCreatedAt, todayStart)))
                .build();
    }
}
