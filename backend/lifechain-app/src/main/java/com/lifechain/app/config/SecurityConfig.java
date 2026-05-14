package com.lifechain.app.config;

import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.auth.service.JwtClaims;
import com.lifechain.auth.service.JwtService;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.enums.AccountStatusEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.infra.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置
 * <p>
 * 基于JWT的无状态认证方案，配置URL级别的访问控制规则、
 * CORS跨域策略、会话管理策略和密码编码器。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final AccountMapper accountMapper;
    private final RedisService redisService;

    /** 账户状态 Redis 缓存前缀，TTL 5分钟 */
    private static final String ACCOUNT_STATUS_CACHE_PREFIX = "account:status:";
    private static final long ACCOUNT_STATUS_CACHE_TTL_SECONDS = 300L;

    /** Token 黑名单 Redis 前缀（退出登录时写入） */
    static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 公开访问的端点路径（始终公开）
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/sms/send",
            "/api/public/**",
            "/public/verify",
            "/api/payments/wechat/callback",
            "/api/payments/alipay/callback",
            "/api/market/**",
            "/actuator/health"
    };

    /**
     * 仅开发环境公开的文档端点
     */
    private static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * 安全过滤链配置
     * <p>
     * 禁用CSRF（无状态API不需要）、禁用表单登录和HTTP Basic、
     * 配置URL权限规则、添加JWT认证过滤器。
     * </p>
     *
     * @param http HttpSecurity 构建器
     * @return 安全过滤链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                        if (swaggerEnabled) {
                            auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
                        }
                        auth.requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
                            .requestMatchers("/api/regulator/**").hasAnyRole("REGULATOR", "PLATFORM_ADMIN")
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * JWT认证过滤器
     * <p>
     * 从请求头提取Bearer Token，解析JWT获取用户信息，
     * 设置Spring Security认证上下文和业务用户上下文。
     * </p>
     *
     * @return JWT认证过滤器实例
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * 密码编码器
     *
     * @return BCrypt密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 跨域配置
     * <p>
     * 开发环境允许所有来源，生产环境应通过配置限制允许的域名。
     * </p>
     *
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                "X-Request-Id",
                "X-Idempotent-Key"
        ));
        configuration.setExposedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                "X-Request-Id",
                "X-Trace-Id"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * JWT认证过滤器实现
     * <p>
     * 拦截每个请求，从Authorization头提取Bearer Token，
     * 通过JwtService解析令牌，将用户信息设置到Spring Security上下文
     * 和业务UserContext中。无Token时不拦截，交给后续过滤链处理。
     * </p>
     */
    public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {
            try {
                String token = extractToken(request);
                if (token != null && jwtService.validateToken(token)) {

                    // 检查 Token 是否已被加入黑名单（退出登录）
                    String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
                    if (redisService.hasKey(blacklistKey)) {
                        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                ErrorCodeEnum.TOKEN_INVALID.getCode(), "Token已失效，请重新登录");
                        return;
                    }

                    JwtClaims claims = jwtService.parseToken(token);

                    // 优先从 Redis 缓存读取账户状态，减少数据库查询
                    String cacheKey = ACCOUNT_STATUS_CACHE_PREFIX + claims.getUserId();
                    String cachedStatus = redisService.getString(cacheKey);
                    String status;
                    if (cachedStatus != null) {
                        status = cachedStatus;
                    } else {
                        AccountEntity account = accountMapper.selectById(claims.getUserId());
                        if (account == null) {
                            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                                    ErrorCodeEnum.ACCOUNT_NOT_FOUND.getCode(), "账户不存在");
                            return;
                        }
                        status = account.getStatus();
                        redisService.set(cacheKey, status, ACCOUNT_STATUS_CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
                    }

                    if (AccountStatusEnum.ACCOUNT_FROZEN.getCode().equals(status)) {
                        writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                ErrorCodeEnum.ACCOUNT_FROZEN.getCode(), "账户已冻结，禁止操作");
                        return;
                    }
                    if (AccountStatusEnum.ACCOUNT_DISABLED.getCode().equals(status)) {
                        writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                                ErrorCodeEnum.ACCOUNT_LOCKED.getCode(), "账户已停用");
                        return;
                    }

                    setSecurityContext(claims);
                    setUserContext(claims);
                }
            } catch (Exception e) {
                log.warn("JWT认证失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }

            filterChain.doFilter(request, response);
        }

        /**
         * 从请求头中提取JWT令牌
         *
         * @param request HTTP请求
         * @return JWT令牌字符串，无效时返回null
         */
        private String extractToken(HttpServletRequest request) {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith(BEARER_PREFIX)) {
                return header.substring(BEARER_PREFIX.length());
            }
            return null;
        }

        /**
         * 设置Spring Security认证上下文
         *
         * @param claims JWT载荷信息
         */
        private void setSecurityContext(JwtClaims claims) {
            List<SimpleGrantedAuthority> authorities = claims.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            claims.getUserId(), null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        /**
         * 设置业务用户上下文
         *
         * @param claims JWT载荷信息
         */
        private void setUserContext(JwtClaims claims) {
            UserContext.set(UserContext.UserInfo.builder()
                    .userId(claims.getUserId())
                    .accountNo(claims.getAccountNo())
                    .accountType(claims.getAccountType())
                    .roles(claims.getRoles())
                    .build());
        }

        private void writeErrorResponse(HttpServletResponse response, int httpStatus, String code, String message) throws IOException {
            response.setStatus(httpStatus);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String body = String.format("{\"code\":\"%s\",\"message\":\"%s\",\"success\":false}", code, message);
            response.getWriter().write(body);
            response.getWriter().flush();
        }
    }
}
