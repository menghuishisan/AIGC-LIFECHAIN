package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 冻结事后复核请求
 */
@Data
public class ReviewFreezeRequest implements Serializable {

    /** 冻结编号 */
    @NotBlank(message = "冻结编号不能为空")
    private String freezeNo;

    /** 是否通过 */
    @NotNull(message = "审核结论不能为空")
    private Boolean approved;

    /** 复核备注 */
    private String reviewNote;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
