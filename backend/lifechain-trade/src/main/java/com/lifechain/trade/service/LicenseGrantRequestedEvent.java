package com.lifechain.trade.service;

/** 支付事务提交后，请求异步启动订单授权。 */
public record LicenseGrantRequestedEvent(String orderNo) {}
