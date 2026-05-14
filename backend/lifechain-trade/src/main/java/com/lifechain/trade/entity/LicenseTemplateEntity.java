package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 授权模板实体
 * <p>
 * 对应数据库表 {@code license_template}，存储授权许可的标准模板信息，
 * 包括授权类型、范围描述、有效期天数、价格等字段。
 * 授权模板用于快速创建上架记录，确保授权条款的一致性和可追溯性。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("license_template")
public class LicenseTemplateEntity extends BaseEntity {

    /** 模板名称 */
    @TableField("template_name")
    private String templateName;

    /** 模板编码（唯一标识） */
    @TableField("template_code")
    private String templateCode;

    /** 授权类型（PERSONAL_USE/COMMERCIAL_USE/EXCLUSIVE） */
    @TableField("license_type")
    private String licenseType;

    /** 范围描述 */
    @TableField("scope_description")
    private String scopeDescription;

    /** 有效天数 */
    @TableField("duration_days")
    private Integer durationDays;

    /** 价格（单位：分） */
    @TableField("price_amount")
    private Long priceAmount;

    /** 币种 */
    @TableField("currency")
    private String currency;

    /** 状态（ACTIVE/INACTIVE） */
    @TableField("status")
    private String status;

    /** 描述 */
    @TableField("description")
    private String description;
}
