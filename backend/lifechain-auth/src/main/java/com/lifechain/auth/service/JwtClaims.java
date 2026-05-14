package com.lifechain.auth.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JWT令牌载荷信息
 * <p>
 * 从JWT令牌中解析出的用户核心信息，包括用户ID、账户编号、账户类型和角色列表。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {

    /** 用户ID */
    private Long userId;

    /** 账户编号 */
    private String accountNo;

    /** 账户类型 */
    private String accountType;

    /** 角色列表 */
    private List<String> roles;
}
