package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单列表视图对象
 * <p>
 * 用于订单列表展示的精简信息，包括订单编号、作品标题、状态、金额等。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class OrderListVO implements Serializable {

    /** 订单编号 */
    private String orderNo;

    /** 作品标题 */
    private String workTitle;

    /** 订单状态 */
    private String orderStatus;

    /** 实付金额（单位：分） */
    private Long payAmount;

    /** 支付渠道 */
    private String payChannel;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
