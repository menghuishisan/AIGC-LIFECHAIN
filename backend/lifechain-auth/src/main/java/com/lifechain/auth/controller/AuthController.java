package com.lifechain.auth.controller;

import com.lifechain.auth.dto.ChangePasswordRequest;
import com.lifechain.auth.dto.LoginRequest;
import com.lifechain.auth.dto.LoginResponse;
import com.lifechain.auth.dto.RegisterRequest;
import com.lifechain.auth.service.AccountService;
import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器。
 * <p>
 * 提供注册、登录、刷新令牌、退出登录、修改密码与短信验证码发送能力。
 * 注册和登录无需身份认证，退出登录和修改密码要求当前用户已登录。
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、退出、修改密码与 Token 刷新")
public class AuthController {

    private final AccountService accountService;

    /**
     * 发送短信验证码。
     *
     * @param mobile 手机号
     * @param request HTTP 请求
     * @return 空响应
     */
    @PostMapping("/sms/send")
    @Operation(summary = "发送短信验证码", description = "注册前调用，60 秒内同一手机号只能发送一次，验证码 5 分钟有效")
    public ApiResponse<Void> sendSmsCode(
            @RequestParam @NotBlank @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String mobile,
            HttpServletRequest request) {
        accountService.sendSmsCode(mobile, request.getRemoteAddr());
        return ApiResponse.success(null);
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 登录响应
     */
    @PostMapping("/register")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "用户注册", description = "通过手机号、短信验证码和密码注册新账户")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到注册请求");
        LoginResponse response = accountService.register(request);
        return ApiResponse.success(response);
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过手机号和密码登录，返回 accessToken 和 refreshToken")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求");
        LoginResponse response = accountService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 刷新令牌。
     *
     * @param body 请求体
     * @return 登录响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "使用 refreshToken 换取新的 accessToken，并轮换 refreshToken")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.fail(com.lifechain.common.enums.ErrorCodeEnum.PARAM_INVALID, "refreshToken不能为空");
        }
        LoginResponse response = accountService.refreshToken(refreshToken);
        return ApiResponse.success(response);
    }

    /**
     * 退出登录。
     *
     * @param authHeader Authorization 请求头
     * @return 空响应
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "将当前 Token 加入黑名单，使其立即失效")
    public ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            accountService.logout(token);
        }
        return ApiResponse.success(null);
    }

    /**
     * 修改密码。
     *
     * @param request 修改密码请求
     * @return 空响应
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "校验原密码后修改为新密码")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long accountId = UserContext.getUserId();
        accountService.changePassword(accountId, request);
        return ApiResponse.success(null);
    }
}
