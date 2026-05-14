package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 处理风险事件请求
 * <p>
 * 用于管理员对风险事件进行处理操作，提供处理结果摘要和原因码。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class HandleRiskEventRequest implements Serializable {

    /** 风险编号 */
    @NotBlank(message = "风险编号不能为空")
    private String riskNo;

    /** 处理动作（CONFIRM/RELEASE/FREEZE） */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    /** 结果摘要 */
    @Size(max = 512, message = "结果摘要最长512个字符")
    private String resultSummary;

    /** 原因码 */
    private String reasonCode;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
