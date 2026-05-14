package com.lifechain.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 账户角色实体
 * <p>
 * 对应数据库表 {@code account_role}，存储账户与角色的绑定关系，
 * 一个账户可拥有多个角色（如 BUYER、CREATOR），支持授权人和授权时间记录。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account_role")
public class AccountRoleEntity extends BaseEntity {

    /** 账户ID */
    @TableField("account_id")
    private Long accountId;

    /** 角色编码（CREATOR/BUYER/PLATFORM_ADMIN/REGULATOR） */
    @TableField("role_code")
    private String roleCode;

    /** 状态（ACTIVE/INACTIVE） */
    @TableField("status")
    private String status;

    /** 授予人ID */
    @TableField("granted_by")
    private Long grantedBy;

    /** 授予时间（UTC） */
    @TableField("granted_time")
    private LocalDateTime grantedTime;
}
