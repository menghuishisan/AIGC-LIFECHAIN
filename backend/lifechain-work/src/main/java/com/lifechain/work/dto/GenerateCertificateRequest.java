package com.lifechain.work.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 生成证书请求
 */
@Data
public class GenerateCertificateRequest implements Serializable {

    @NotBlank(message = "确权编号不能为空")
    private String claimNo;

    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
