package com.lifechain.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RetrySettlementRequest {
    @NotBlank(message = "结算编号不能为空")
    private String settleNo;
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
