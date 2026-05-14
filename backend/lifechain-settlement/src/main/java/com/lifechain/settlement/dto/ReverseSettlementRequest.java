package com.lifechain.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReverseSettlementRequest {
    @NotBlank(message = "结算编号不能为空")
    private String settleNo;
    @NotBlank(message = "逆分账原因不能为空")
    private String reason;
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
