package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 支付结果视图对象
 * <p>
 * 返回支付下单后的结果信息，包括预付单ID、前端调起支付所需参数和扫码支付链接。
 * 前端根据 payParams 调起对应渠道的支付控件。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class PayResultVO implements Serializable {

    /** 订单编号 */
    private String orderNo;

    /** 预支付交易会话标识 */
    private String prepayId;

    /** 前端调起支付所需参数（渠道特有） */
    private Map<String, String> payParams;

    /** 支付链接（扫码支付时使用） */
    private String payUrl;
}
