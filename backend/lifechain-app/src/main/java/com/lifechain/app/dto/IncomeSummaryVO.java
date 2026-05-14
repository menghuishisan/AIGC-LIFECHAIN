package com.lifechain.app.dto;

import lombok.Data;

/**
 * 收益汇总视图对象
 */
@Data
public class IncomeSummaryVO {
    private Long totalIncome;
    private Long totalSuccessCount;
    private Long totalCount;
}
