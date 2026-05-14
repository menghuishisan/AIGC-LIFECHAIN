package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 解冻申请请求
 * <p>
 * 用于发起解冻操作，需提供冻结编号和解冻原因。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class UnfreezeRequest implements Serializable {

    /** 冻结编号 */
    @NotBlank(message = "冻结编号不能为空")
    private String freezeNo;

    /** 解冻原因 */
    @NotBlank(message = "解冻原因不能为空")
    @Size(max = 512, message = "解冻原因最长512个字符")
    private String unfreezeReason;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
