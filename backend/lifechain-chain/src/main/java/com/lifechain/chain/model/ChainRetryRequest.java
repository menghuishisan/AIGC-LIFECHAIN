package com.lifechain.chain.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 链交易重试请求 DTO
 */
@Data
public class ChainRetryRequest {
    @NotBlank(message = "链交易哈希不能为空")
    private String txHash;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
