package com.lifechain.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 主体信息实体
 * <p>
 * 对应数据库表 {@code subject_profile}，存储实名认证后的主体信息，
 * 包括个人真实姓名/企业名称、证件信息、企业信用代码等。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subject_profile")
public class SubjectProfileEntity extends BaseEntity {

    /** 主体编号 */
    @TableField("subject_no")
    private String subjectNo;

    /** 关联账户ID */
    @TableField("account_id")
    private Long accountId;

    /** 主体类型（PERSONAL/ENTERPRISE） */
    @TableField("subject_type")
    private String subjectType;

    /** 真实姓名/企业名称 */
    @TableField("real_name")
    private String realName;

    /** 证件类型 */
    @TableField("id_card_type")
    private String idCardType;

    /** 证件号码 */
    @TableField("id_card_no")
    private String idCardNo;

    /** 企业统一社会信用代码 */
    @TableField("enterprise_code")
    private String enterpriseCode;

    /** 企业联系人 */
    @TableField("contact_name")
    private String contactName;

    /** 联系电话 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 认证材料文件地址 */
    @TableField("auth_material_url")
    private String authMaterialUrl;
}
