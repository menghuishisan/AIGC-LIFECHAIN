package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风险事件视图对象
 * <p>
 * 展示风险事件完整信息，包含风险目标、状态、等级、描述及处理结果。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class RiskEventVO implements Serializable {

    /** 风险编号 */
    private String riskNo;

    /** 目标类型 */
    private String targetType;

    /** 目标编号 */
    private String targetNo;

    /** 风险等级 */
    private String riskLevel;

    /** 风险类型 */
    private String riskType;

    /** 风险描述 */
    private String riskDescription;

    /** 风险状态 */
    private String status;

    /** 结果摘要 */
    private String resultSummary;

    /** 解决时间 */
    private LocalDateTime resolveTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
