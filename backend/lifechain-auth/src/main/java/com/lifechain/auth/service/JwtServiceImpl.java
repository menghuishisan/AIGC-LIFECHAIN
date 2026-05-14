package com.lifechain.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT令牌服务实现
 * <p>
 * 基于JJWT库实现JWT令牌的生成、解析和校验。
 * 使用HS512算法签名，密钥和过期时间通过配置文件注入。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    /** JWT签名密钥 */
    @Value("${lifechain.jwt.secret}")
    private String secret;

    /** JWT过期时间（小时） */
    @Value("${lifechain.jwt.expire-hours:24}")
    private int expireHours;

    /** HS512 要求密钥至少 64 字节 */
    private static final int HS512_MIN_KEY_BYTES = 64;

    @PostConstruct
    public void validateSecret() {
        int len = secret.getBytes(StandardCharsets.UTF_8).length;
        if (len < HS512_MIN_KEY_BYTES) {
            throw new IllegalStateException(
                "[LifeChain] JWT secret 长度不足（当前 " + len + " 字节），HS512 要求至少 64 字节，请修改 LIFECHAIN_JWT_SECRET 环境变量");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateToken(Long accountId, String accountNo, String accountType, List<String> roles) {
        log.info("生成JWT令牌，accountId={}, accountNo={}", accountId, accountNo);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + (long) expireHours * 3600 * 1000);

        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("accountNo", accountNo)
                .claim("accountType", accountType)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JwtClaims parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setUserId(Long.parseLong(claims.getSubject()));
        jwtClaims.setAccountNo(claims.get("accountNo", String.class));
        jwtClaims.setAccountType(claims.get("accountType", String.class));

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        jwtClaims.setRoles(roles);

        return jwtClaims;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT令牌格式错误: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT签名验证失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT令牌为空: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 获取签名密钥
     *
     * @return HMAC-SHA密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public long getExpireSeconds() {
        return (long) expireHours * 3600;
    }

    @Override
    public long getRemainingSeconds(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            long expireMs = claims.getExpiration().getTime();
            long remaining = (expireMs - System.currentTimeMillis()) / 1000;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }
}
