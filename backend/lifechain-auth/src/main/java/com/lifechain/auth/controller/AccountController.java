package com.lifechain.auth.controller;

import com.lifechain.auth.dto.AccountProfileVO;
import com.lifechain.auth.dto.AuthSubmitRequest;
import com.lifechain.auth.dto.UpdateProfileRequest;
import com.lifechain.auth.service.AccountService;
import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 账户控制器
 * <p>
 * 处理当前登录用户的个人资料查询、更新及实名认证提交请求。
 * 所有接口均需要用户登录，通过 {@link UserContext} 获取当前用户信息。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "账户管理", description = "个人资料与实名认证")
public class AccountController {

    private final AccountService accountService;

    /**
     * 获取当前用户资料
     *
     * @return 账户详情
     */
    @GetMapping("/profile")
    @Operation(summary = "获取个人资料", description = "获取当前登录用户的账户详情，包含主体信息、DID信息和角色列表")
    public ApiResponse<AccountProfileVO> getProfile() {
        Long accountId = UserContext.getUserId();
        log.info("查询个人资料，accountId={}", accountId);
        AccountProfileVO profile = accountService.getProfile(accountId);
        return ApiResponse.success(profile);
    }

    /**
     * 更新当前用户资料
     *
     * @param request 更新请求
     * @return 成功响应
     */
    @PutMapping("/profile")
    @Idempotent
    @Operation(summary = "更新个人资料", description = "更新当前登录用户的昵称、邮箱、头像等信息")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long accountId = UserContext.getUserId();
        log.info("更新个人资料，accountId={}", accountId);
        accountService.updateProfile(accountId, request);
        return ApiResponse.success();
    }

    /**
     * 提交实名认证
     *
     * @param request 认证提交请求
     * @return 成功响应
     */
    @PostMapping("/auth/submit")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "提交实名认证", description = "提交主体信息进行实名认证，提交后状态变为AUTH_PENDING")
    public ApiResponse<Void> submitAuth(@Valid @RequestBody AuthSubmitRequest request) {
        Long accountId = UserContext.getUserId();
        log.info("提交实名认证，accountId={}", accountId);
        accountService.submitAuth(accountId, request);
        return ApiResponse.success();
    }
}
