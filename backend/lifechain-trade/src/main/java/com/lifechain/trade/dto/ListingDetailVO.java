package com.lifechain.trade.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 上架详情视图对象
 * <p>
 * 返回作品上架的完整信息，包括上架编号、作品信息、授权类型、价格及状态等。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ListingDetailVO implements Serializable {

    /** 上架编号 */
    private String listingNo;

    /** 作品编号 */
    private String workNo;

    /** 作品标题 */
    private String workTitle;

    /** 封面地址 */
    private String coverUrl;

    /** 授权类型 */
    private String licenseType;

    /** 价格（单位：分） */
    private Long priceAmount;

    /** 币种 */
    private String currency;

    /** 范围描述 */
    private String scopeDescription;

    /** 有效天数 */
    private Integer durationDays;

    /** 上架状态 */
    private String status;

    /** 上架时间 */
    private LocalDateTime listTime;

    /** 创作者账户编号 */
    private String creatorAccountNo;
}
