package com.lifechain.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifechain.app.dto.DistributionVO;
import com.lifechain.app.dto.StatsCountVO;
import com.lifechain.app.dto.StatsOverviewVO;
import com.lifechain.app.dto.TrendPointVO;
import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.regulator.entity.RiskEventEntity;
import com.lifechain.regulator.mapper.RiskEventMapper;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.work.entity.ClaimApplicationEntity;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.ClaimApplicationMapper;
import com.lifechain.work.mapper.VerifyQueryLogMapper;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final AccountMapper accountMapper;
    private final WorkMapper workMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final ClaimApplicationMapper claimApplicationMapper;
    private final VerifyQueryLogMapper verifyQueryLogMapper;
    private final RiskEventMapper riskEventMapper;

    public StatsOverviewVO getOverview() {
        return StatsOverviewVO.builder()
                .totalAccounts(accountMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalWorks(workMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalOrders(tradeOrderMapper.selectCount(new LambdaQueryWrapper<>()))
                .totalSettlements(settlementRecordMapper.selectCount(new LambdaQueryWrapper<>()))
                .build();
    }

    public List<TrendPointVO> getClaimsTrend(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();
        List<ClaimApplicationEntity> records = claimApplicationMapper.selectList(
                new LambdaQueryWrapper<ClaimApplicationEntity>()
                        .ge(ClaimApplicationEntity::getCreatedAt, start));
        return groupByDate(records, ClaimApplicationEntity::getCreatedAt, days);
    }

    public List<TrendPointVO> getOrdersTrend(int days) {
        LocalDateTime start = LocalDate.now().minusDays(days).atStartOfDay();
        List<TradeOrderEntity> records = tradeOrderMapper.selectList(
                new LambdaQueryWrapper<TradeOrderEntity>()
                        .ge(TradeOrderEntity::getCreatedAt, start));
        return groupByDate(records, TradeOrderEntity::getCreatedAt, days);
    }

    public StatsCountVO getSettlementsCount() {
        return StatsCountVO.builder()
                .total(settlementRecordMapper.selectCount(new LambdaQueryWrapper<>()))
                .build();
    }

    public StatsCountVO getVerifyCount() {
        return StatsCountVO.builder()
                .total(verifyQueryLogMapper.selectCount(new LambdaQueryWrapper<>()))
                .build();
    }

    public StatsCountVO getRiskCount() {
        return StatsCountVO.builder()
                .total(riskEventMapper.selectCount(new LambdaQueryWrapper<>()))
                .build();
    }

    public DistributionVO getDistribution() {
        Map<String, Long> worksByStatus = workMapper.selectList(new LambdaQueryWrapper<WorkEntity>()
                        .select(WorkEntity::getStatus))
                .stream().collect(Collectors.groupingBy(WorkEntity::getStatus, Collectors.counting()));

        Map<String, Long> ordersByStatus = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrderEntity>()
                        .select(TradeOrderEntity::getOrderStatus))
                .stream().collect(Collectors.groupingBy(TradeOrderEntity::getOrderStatus, Collectors.counting()));

        Map<String, Long> accountsByType = accountMapper.selectList(new LambdaQueryWrapper<AccountEntity>()
                        .select(AccountEntity::getAccountType))
                .stream().collect(Collectors.groupingBy(AccountEntity::getAccountType, Collectors.counting()));

        Map<String, Long> riskByLevel = riskEventMapper.selectList(new LambdaQueryWrapper<RiskEventEntity>()
                        .select(RiskEventEntity::getRiskLevel))
                .stream().filter(e -> e.getRiskLevel() != null)
                .collect(Collectors.groupingBy(RiskEventEntity::getRiskLevel, Collectors.counting()));

        return DistributionVO.builder()
                .worksByStatus(worksByStatus)
                .ordersByStatus(ordersByStatus)
                .accountsByType(accountsByType)
                .riskByLevel(riskByLevel)
                .build();
    }

    private <T> List<TrendPointVO> groupByDate(List<T> records,
            java.util.function.Function<T, LocalDateTime> dateExtractor, int days) {
        Map<LocalDate, Long> countMap = records.stream()
                .filter(r -> dateExtractor.apply(r) != null)
                .collect(Collectors.groupingBy(r -> dateExtractor.apply(r).toLocalDate(), Collectors.counting()));

        List<TrendPointVO> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            trend.add(TrendPointVO.builder()
                    .date(date.toString())
                    .count(countMap.getOrDefault(date, 0L))
                    .build());
        }
        return trend;
    }
}
