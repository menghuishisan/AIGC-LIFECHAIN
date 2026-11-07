package com.lifechain.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.app.dto.IncomeItemVO;
import com.lifechain.app.dto.IncomeSummaryVO;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.settlement.entity.SettlementItemEntity;
import com.lifechain.settlement.mapper.SettlementItemMapper;
import com.lifechain.settlement.dto.IncomeTrendPoint;
import com.lifechain.common.util.DateTimeUtil;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final SettlementItemMapper settlementItemMapper;

    /** 最近三十个 UTC 自然日的净收益，空日期补零，避免把最近一页明细误作整月趋势。 */
    public List<IncomeTrendPoint> getIncomeTrend(Long accountId) {
        var end = DateTimeUtil.nowUtc().toLocalDate().plusDays(1);
        var start = end.minusDays(30);
        var points = settlementItemMapper.selectConfirmedIncomeTrend(accountId, start.atStartOfDay(), end.atStartOfDay());
        var amounts = points.stream().collect(Collectors.toMap(IncomeTrendPoint::getDate, IncomeTrendPoint::getAmount));
        return start.datesUntil(end).map(date -> {
            var point = new IncomeTrendPoint();
            point.setDate(date);
            point.setAmount(amounts.getOrDefault(date, 0L));
            return point;
        }).toList();
    }

    public IncomeSummaryVO getIncomeSummary(Long accountId) {
        LambdaQueryWrapper<SettlementItemEntity> wrapper = new LambdaQueryWrapper<SettlementItemEntity>()
                .eq(SettlementItemEntity::getAccountId, accountId);
        Long totalItems = settlementItemMapper.selectCount(wrapper);

        var successList = settlementItemMapper.selectConfirmedIncome(accountId);
        long successItems = successList.size();
        long totalIncome = successList.stream().mapToLong(SettlementItemEntity::getAmount).sum();

        IncomeSummaryVO summary = new IncomeSummaryVO();
        summary.setTotalIncome(totalIncome);
        summary.setTotalSuccessCount(successItems);
        summary.setTotalCount(totalItems);
        return summary;
    }

    public PageResult<IncomeItemVO> listIncomeDetails(Long accountId, PageQuery query) {
        Page<SettlementItemEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<SettlementItemEntity> result = settlementItemMapper.selectIncomePage(page, accountId);
        return PageResult.of(result.getRecords().stream().map(IncomeItemVO::fromEntity).toList(),
                result.getTotal(), query.getPageNo(), query.getPageSize());
    }
}
