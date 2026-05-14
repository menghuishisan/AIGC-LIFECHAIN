package com.lifechain.settlement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 结算模板明细实体
 * <p>
 * 对应数据库表 {@code settle_template_item}，记录模板中每个角色的分账比例配置。
 * 每个模板可包含多个明细行，分别定义创作者、平台等角色的分账比例。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("settle_template_item")
public class SettleTemplateItemEntity extends BaseEntity {

    /** 结算模板ID */
    @TableField("template_id")
    private Long templateId;

    /** 角色类型（CREATOR/PLATFORM/OTHER） */
    @TableField("role_type")
    private String roleType;

    /** 分账比例（如0.8000表示80%） */
    @TableField("ratio")
    private BigDecimal ratio;

    /** 描述 */
    @TableField("description")
    private String description;
}
