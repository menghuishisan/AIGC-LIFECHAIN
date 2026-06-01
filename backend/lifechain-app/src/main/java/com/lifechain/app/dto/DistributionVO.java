package com.lifechain.app.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 分布统计视图对象
 * <p>
 * 提供作品类型、订单状态、风险状态三个维度的数量分布，
 * 字段名与前端 Dashboard 饼图卡片一一对应。
 * </p>
 */
@Data
@Builder
public class DistributionVO implements Serializable {

    /** 作品类型分布（按 work.workType 聚合） */
    private Map<String, Long> workTypeDistribution;

    /** 订单状态分布（按 trade_order.orderStatus 聚合） */
    private Map<String, Long> orderStatusDistribution;

    /** 风险状态分布（按 risk_event.status 聚合） */
    private Map<String, Long> riskStatusDistribution;
}
