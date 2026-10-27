package com.lifechain.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.app.dto.IncomeItemVO;
import com.lifechain.app.dto.IncomeSummaryVO;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.settlement.entity.SettlementItemEntity;
import com.lifechain.settlement.mapper.SettlementItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final SettlementItemMapper settlementItemMapper;

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
