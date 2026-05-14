package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作品上架实体
 * <p>
 * 对应数据库表 {@code work_listing}，记录作品在交易市场的上架信息，
 * 包括上架编号、关联作品、授权模板、价格、审核状态及上下架时间等。
 * 每个作品在同一时间只能有一条有效的上架记录。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_listing")
public class WorkListingEntity extends BaseEntity {

    /** 上架编号（对外唯一标识） */
    @TableField("listing_no")
    private String listingNo;

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 创作者账户ID */
    @TableField("creator_account_id")
    private Long creatorAccountId;

    /** 授权模板ID */
    @TableField("license_template_id")
    private Long licenseTemplateId;

    /** 授权类型（PERSONAL_USE/COMMERCIAL_USE/EXCLUSIVE） */
    @TableField("license_type")
    private String licenseType;

    /** 价格（单位：分） */
    @TableField("price_amount")
    private Long priceAmount;

    /** 币种 */
    @TableField("currency")
    private String currency;

    /** 上架状态（PENDING_REVIEW/LISTED/UNLISTED/REJECTED/FROZEN） */
    @TableField("status")
    private String status;

    /** 审核状态 */
    @TableField("review_status")
    private String reviewStatus;

    /** 审核人ID */
    @TableField("reviewer_id")
    private Long reviewerId;

    /** 审核意见 */
    @TableField("review_comment")
    private String reviewComment;

    /** 上架时间（UTC） */
    @TableField("list_time")
    private LocalDateTime listTime;

    /** 下架时间（UTC） */
    @TableField("unlist_time")
    private LocalDateTime unlistTime;

    /** 范围描述 */
    @TableField("scope_description")
    private String scopeDescription;

    /** 有效天数 */
    @TableField("duration_days")
    private Integer durationDays;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
