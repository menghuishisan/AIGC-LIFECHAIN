package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员订单查询条件
 * <p>
 * 用于管理员后台全量订单列表的筛选条件，不按当前登录用户归属过滤。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class AdminOrderQuery implements Serializable {

    /** 订单编号（模糊匹配） */
    private String orderNo;

    /** 买方账户编号 */
    private String buyerAccountNo;

    /** 创作者账户编号 */
    private String creatorAccountNo;

    /** 订单状态 */
    private String orderStatus;

    /** 支付渠道 */
    private String payChannel;

    /** 授权编号 */
    private String licenseNo;

    /** 作品编号 */
    private String workNo;

    /** 查询开始时间 */
    private LocalDateTime dateFrom;

    /** 查询结束时间 */
    private LocalDateTime dateTo;
}
