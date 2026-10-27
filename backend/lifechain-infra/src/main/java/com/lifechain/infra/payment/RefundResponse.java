package com.lifechain.infra.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款响应DTO
 * <p>
 * 统一封装退款操作的返回结果。
 * thirdRefundNo 为第三方渠道生成的退款流水号，用于后续查询和对账。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    /** 退款单号（业务侧） */
    private String refundNo;

    /** 第三方退款流水号 */
    private String thirdRefundNo;

    /** 是否成功 */
    private boolean success;

    /** 已受理但资金结果尚未确认，需要复用原退款编号查询 */
    private boolean pending;

    /** 错误信息（失败时填充） */
    private String errorMsg;
}
