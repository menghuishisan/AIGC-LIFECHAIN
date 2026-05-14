package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 争议处理请求
 * <p>
 * 用于管理员对争议案件进行处理，包含处理动作和处理意见。
 * 支持的动作：ACCEPT（受理）、EVIDENCE_PENDING（要求补充证据）、
 * REVIEW（进入审查）、RESOLVE（解决）、REJECT（驳回）、CLOSE（关闭）。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class DisputeProcessRequest implements Serializable {

    /** 案件编号 */
    @NotBlank(message = "案件编号不能为空")
    private String caseNo;

    /** 处理动作（ACCEPT/EVIDENCE_PENDING/REVIEW/RESOLVE/REJECT/CLOSE） */
    @NotBlank(message = "处理动作不能为空")
    private String action;

    /** 处理意见 */
    @Size(max = 512, message = "处理意见最长512个字符")
    private String comment;

    /** 结果摘要（解决/驳回/关闭时提供） */
    @Size(max = 512, message = "结果摘要最长512个字符")
    private String resultSummary;

    /** 原因码 */
    private String reasonCode;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
