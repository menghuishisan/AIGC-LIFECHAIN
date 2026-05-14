package com.lifechain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应
 * <p>
 * 登录成功后返回的令牌和用户基本信息，包含JWT访问令牌、过期时间、
 * 账户编号、昵称、账户类型及角色列表。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse implements Serializable {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌（用于无感续期，有效期7天） */
    private String refreshToken;

    /** 令牌类型 */
    @Builder.Default
    private String tokenType = "Bearer";

    /** 访问令牌过期时间（秒） */
    private Long expiresIn;

    /** 账户编号 */
    private String accountNo;

    /** 昵称 */
    private String nickname;

    /** 账户类型 */
    private String accountType;

    /** 角色列表 */
    private List<String> roles;
}
