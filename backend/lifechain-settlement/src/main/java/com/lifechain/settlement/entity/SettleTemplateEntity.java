package com.lifechain.settlement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 结算模板实体
 * <p>
 * 对应数据库表 {@code settle_template}，定义分账比例分配模板，
 * 管理员可创建和维护多套分账模板，绑定到具体作品的结算规则中使用。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("settle_template")
public class SettleTemplateEntity extends BaseEntity {

    /** 模板名称 */
    @TableField("template_name")
    private String templateName;

    /** 模板编码（对外唯一标识） */
    @TableField("template_code")
    private String templateCode;

    /** 描述 */
    @TableField("description")
    private String description;

    /** 状态（ACTIVE/INACTIVE） */
    @TableField("status")
    private String status;
}
