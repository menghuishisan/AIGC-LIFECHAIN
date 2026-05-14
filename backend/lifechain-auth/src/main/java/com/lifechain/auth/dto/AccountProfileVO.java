package com.lifechain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 账户详情视图对象
 * <p>
 * 聚合展示账户基本信息、主体信息、DID信息及角色列表。
 * 敏感字段（手机号、身份证号等）按角色权限进行脱敏处理。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountProfileVO implements Serializable {

    /** 账户编号 */
    private String accountNo;

    /** 手机号（脱敏） */
    private String mobile;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 头像地址 */
    private String avatarUrl;

    /** 账户类型 */
    private String accountType;

    /** 账户状态 */
    private String status;

    /** 认证状态 */
    private String authStatus;

    /** 主体信息 */
    private SubjectInfoVO subjectInfo;

    /** DID信息 */
    private DidInfoVO didInfo;

    /** 角色列表 */
    private List<String> roles;

    /** 当前允许的操作列表 */
    private List<String> allowedActions;
}
