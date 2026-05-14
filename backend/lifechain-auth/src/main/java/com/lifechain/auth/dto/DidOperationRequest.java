package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * DID操作请求（挂起/吊销）
 */
@Data
public class DidOperationRequest implements Serializable {

    @NotBlank(message = "DID编号不能为空")
    private String didNo;

    @NotBlank(message = "操作原因不能为空")
    private String reason;

    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
