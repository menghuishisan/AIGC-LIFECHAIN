package com.lifechain.auth.service;

import java.util.List;

/**
 * JWT令牌服务接口
 * <p>
 * 提供JWT令牌的生成、解析和校验功能，使用HS512算法签名。
 * 令牌中包含用户ID、账户编号、账户类型和角色列表等核心信息。
 * </p>
 *
 * @author LifeChain
 */
public interface JwtService {

    /**
     * 生成JWT令牌
     *
     * @param accountId   用户ID
     * @param accountNo   账户编号
     * @param accountType 账户类型
     * @param roles       角色列表
     * @return JWT令牌字符串
     */
    String generateToken(Long accountId, String accountNo, String accountType, List<String> roles);

    /**
     * 解析JWT令牌
     *
     * @param token JWT令牌字符串
     * @return 令牌载荷信息
     */
    JwtClaims parseToken(String token);

    /**
     * 校验JWT令牌是否有效
     *
     * @param token JWT令牌字符串
     * @return true-有效，false-无效
     */
    boolean validateToken(String token);

    /**
     * 获取令牌过期时间（秒）
     *
     * @return 过期秒数
     */
    long getExpireSeconds();

    /**
     * 获取令牌距过期的剩余秒数
     *
     * @param token JWT令牌字符串
     * @return 剩余秒数，已过期返回0
     */
    long getRemainingSeconds(String token);
}
