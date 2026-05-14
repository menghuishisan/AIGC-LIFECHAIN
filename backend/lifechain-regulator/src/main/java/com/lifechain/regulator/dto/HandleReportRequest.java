package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 处理监管报告请求
 * <p>
 * 用于触发监管报告的生成操作或完成标记。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class HandleReportRequest implements Serializable {

    /** 报告编号 */
    @NotBlank(message = "报告编号不能为空")
    private String reportNo;

    /** 处理动作（GENERATE/COMPLETE/FAIL） */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    /** 报告文件地址（完成时提供） */
    private String reportFileUrl;

    /** 结果说明 */
    private String resultSummary;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
