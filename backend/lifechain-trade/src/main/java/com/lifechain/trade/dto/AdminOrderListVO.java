package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员订单列表视图对象
 * <p>
 * 用于管理员后台订单列表展示，包含订单摘要信息、买卖双方账户编号及关联业务编号。
 * 不返回完整详情结构，仅作列表展示用。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class AdminOrderListVO implements Serializable {

    /** 订单编号 */
    private String orderNo;

    /** 订单状态 */
    private String orderStatus;

    /** 买方账户编号 */
    private String buyerAccountNo;

    /** 创作者账户编号 */
    private String creatorAccountNo;

    /** 作品编号 */
    private String workNo;

    /** 上架编号 */
    private String listingNo;

    /** 授权编号 */
    private String licenseNo;

    /** 支付渠道 */
    private String payChannel;

    /** 实付金额（单位：分） */
    private Long payAmount;

    /** 是否存在退款 */
    private Boolean hasRefund;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
