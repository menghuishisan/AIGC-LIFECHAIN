package com.lifechain.trade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建订单请求
 * <p>
 * 买方选择上架作品并提交下单请求，指定支付渠道。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class CreateOrderRequest implements Serializable {

    /** 上架编号 */
    @NotBlank(message = "上架编号不能为空")
    private String listingNo;

    /** 支付渠道（WECHAT_PAY/ALIPAY） */
    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    /** 幂等请求ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
