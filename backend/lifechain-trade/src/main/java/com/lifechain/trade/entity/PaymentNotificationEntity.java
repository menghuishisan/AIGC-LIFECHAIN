package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 支付通知持久化去重记录；仅保存业务标识及摘要，不落签名或买家个人信息。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_notification")
public class PaymentNotificationEntity extends BaseEntity {
    private String payChannel;
    private String notifyId;
    private String orderNo;
    private String thirdTradeNo;
    private String tradeStatus;
    private String payloadHash;
}
