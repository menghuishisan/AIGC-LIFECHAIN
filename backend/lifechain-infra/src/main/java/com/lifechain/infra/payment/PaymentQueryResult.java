package com.lifechain.infra.payment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 渠道交易查询结果；接口成功和付款成功分别判断，未知状态不得推进订单。 */
@Data
@Builder
public class PaymentQueryResult {
    private String orderNo;
    private String thirdTradeNo;
    private String tradeStatus;
    private Long totalAmount;
    private LocalDateTime payTime;

    public boolean isPaid() {
        return "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
    }
}
