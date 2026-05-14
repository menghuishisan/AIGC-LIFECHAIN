package com.lifechain.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品结算规则视图对象
 * <p>
 * 展示作品绑定的结算规则详情，包括分成比例和生效信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class WorkSettleRuleVO implements Serializable {

    /** 作品编号 */
    private String workNo;

    /** 模板编码 */
    private String templateCode;

    /** 平台分成比例 */
    private BigDecimal platformRatio;

    /** 创作者分成比例 */
    private BigDecimal creatorRatio;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 规则状态 */
    private String ruleStatus;
}
