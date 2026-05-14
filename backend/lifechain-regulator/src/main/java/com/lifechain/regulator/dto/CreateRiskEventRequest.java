package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建风险事件请求
 * <p>
 * 包含风险目标的类型、ID及风险等级和描述等信息，用于新增风险事件记录。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class CreateRiskEventRequest implements Serializable {

    /** 目标类型（ACCOUNT/WORK/ORDER等） */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /** 目标业务编号 */
    @NotBlank(message = "目标编号不能为空")
    private String targetNo;

    /** 风险类型 */
    @NotBlank(message = "风险类型不能为空")
    private String riskType;

    /** 风险等级（LOW/MEDIUM/HIGH/CRITICAL） */
    @NotBlank(message = "风险等级不能为空")
    private String riskLevel;

    /** 风险描述 */
    @Size(max = 2000, message = "风险描述最长2000个字符")
    private String riskDescription;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
