package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 证书模板实体
 * <p>
 * 对应数据库表 {@code certificate_template}，存储证书生成所用的模板信息，
 * 包括模板名称、编码、HTML/JSON内容及启用状态。
 * 证书生成时根据模板编码加载对应模板内容进行渲染。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("certificate_template")
public class CertificateTemplateEntity extends BaseEntity {

    /** 模板名称 */
    @TableField("template_name")
    private String templateName;

    /** 模板编码（唯一） */
    @TableField("template_code")
    private String templateCode;

    /** 模板内容（HTML/JSON） */
    @TableField("template_content")
    private String templateContent;

    /** 状态（ACTIVE/INACTIVE） */
    @TableField("status")
    private String status;

    /** 描述 */
    @TableField("description")
    private String description;
}
