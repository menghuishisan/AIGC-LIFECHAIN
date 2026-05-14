package com.lifechain.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 平台统计概览视图对象
 * <p>
 * 管理后台首页展示的核心业务统计数据，包含各模块的总量统计。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsOverviewVO {

    /** 总账户数 */
    private Long totalAccounts;

    /** 总作品数 */
    private Long totalWorks;

    /** 总订单数 */
    private Long totalOrders;

    /** 总结算单数 */
    private Long totalSettlements;

    /** 总交易金额（分） */
    private Long totalTradeAmount;

    /** 今日新增账户数 */
    private Long todayNewAccounts;

    /** 今日新增订单数 */
    private Long todayNewOrders;
}
