package com.lifechain.settlement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 绑定结算规则请求
 * <p>
 * 将分账规则绑定到指定作品，需指定作品编号、模板编码（可选）以及平台/创作者分成比例。
 * 平台比例与创作者比例之和必须等于1（100%）。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class BindSettleRuleRequest implements Serializable {

    /** 作品编号 */
    @NotBlank(message = "作品编号不能为空")
    private String workNo;

    /** 模板编码（可选，不指定则仅使用平台/创作者比例） */
    private String templateCode;

    /** 平台分成比例（0.0001~1.0000） */
    @NotNull(message = "平台分成比例不能为空")
    @DecimalMin(value = "0.0001", message = "平台分成比例最小为0.0001")
    @DecimalMax(value = "1.0000", message = "平台分成比例最大为1.0000")
    private BigDecimal platformRatio;

    /** 创作者分成比例（0.0001~1.0000） */
    @NotNull(message = "创作者分成比例不能为空")
    @DecimalMin(value = "0.0001", message = "创作者分成比例最小为0.0001")
    @DecimalMax(value = "1.0000", message = "创作者分成比例最大为1.0000")
    private BigDecimal creatorRatio;

    /** 创作者账户编号 */
    @NotBlank(message = "创作者账户编号不能为空")
    private String creatorAccountNo;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
