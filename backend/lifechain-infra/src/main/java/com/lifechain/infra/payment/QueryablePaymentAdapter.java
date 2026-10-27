package com.lifechain.infra.payment;

/** 在统一退款查询之外，支持交易补偿查询与关单的支付渠道能力。 */
public interface QueryablePaymentAdapter extends PaymentAdapter {
    PaymentQueryResult queryPayment(String orderNo);

    void closePayment(String orderNo);
}
