package com.lifechain.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 账户实体
 * <p>
 * 对应数据库表 {@code account}，存储用户的基本账户信息，
 * 包括手机号、密码哈希、账户类型、认证状态、最后登录信息等。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account")
public class AccountEntity extends BaseEntity {

    /** 账户编号（对外唯一标识） */
    @TableField("account_no")
    private String accountNo;

    /** 手机号 */
    @TableField("mobile")
    private String mobile;

    /** 密码哈希（BCrypt） */
    @TableField("password_hash")
    private String passwordHash;

    /** 账户类型（PERSONAL/ENTERPRISE/PLATFORM/REGULATOR） */
    @TableField("account_type")
    private String accountType;

    /** 账户状态（REGISTERED/AUTH_PENDING/AUTH_REJECTED/AUTH_APPROVED/ACCOUNT_FROZEN/ACCOUNT_DISABLED） */
    @TableField("status")
    private String status;

    /** 认证状态 */
    @TableField("auth_status")
    private String authStatus;

    /** 头像地址 */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 邮箱 */
    @TableField("email")
    private String email;

    /** 最后登录时间（UTC） */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /** 冻结前状态（解冻时恢复用） */
    @TableField("previous_status")
    private String previousStatus;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;
}
