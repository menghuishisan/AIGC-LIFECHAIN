package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建监管报告请求
 * <p>
 * 包含报告类型、标题、内容和文件地址，用于新增监管报告。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class CreateReportRequest implements Serializable {

    /** 报告类型 */
    @NotBlank(message = "报告类型不能为空")
    private String reportType;

    /** 报告标题 */
    @NotBlank(message = "报告标题不能为空")
    @Size(max = 256, message = "报告标题最长256个字符")
    private String reportTitle;

    /** 报告内容 */
    private String reportContent;

    /** 报告文件地址 */
    private String reportFileUrl;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
