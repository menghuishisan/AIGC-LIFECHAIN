package com.lifechain.auth.controller;

import com.lifechain.auth.dto.*;
import com.lifechain.auth.service.AccountService;
import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-账户管理控制器
 * <p>
 * 处理管理员对用户实名认证的审核、账户冻结/解冻及账户列表查询等操作。
 * 所有接口需要 PLATFORM_ADMIN 角色权限。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/account")
@RequiredArgsConstructor
@Tag(name = "管理员-账户管理", description = "认证审核、账户冻结/解冻、账户列表")
public class AdminAccountController {

    private final AccountService accountService;

    /**
     * 审核实名认证
     *
     * @param request 审核请求
     * @return 成功响应
     */
    @PostMapping("/auth/review")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "审核实名认证", description = "管理员审核用户的实名认证申请，可批准或驳回")
    public ApiResponse<Void> reviewAuth(@Valid @RequestBody AuthReviewRequest request) {
        Long reviewerId = UserContext.getUserId();
        log.info("管理员审核实名认证，reviewerId={}, accountNo={}", reviewerId, request.getAccountNo());
        accountService.reviewAuth(reviewerId, request);
        return ApiResponse.success();
    }

    /**
     * 冻结账户
     *
     * @param request 冻结请求
     * @return 成功响应
     */
    @PostMapping("/freeze")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "冻结账户", description = "管理员冻结指定账户，冻结后用户无法登录和操作")
    public ApiResponse<Void> freezeAccount(@Valid @RequestBody AccountFreezeRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("管理员冻结账户，operatorId={}, accountNo={}", operatorId, request.getAccountNo());
        accountService.freezeAccount(operatorId, request);
        return ApiResponse.success();
    }

    /**
     * 解冻账户
     *
     * @param request 解冻请求
     * @return 成功响应
     */
    @PostMapping("/unfreeze")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "解冻账户", description = "管理员解冻指定账户，恢复正常使用")
    public ApiResponse<Void> unfreezeAccount(@Valid @RequestBody AccountFreezeRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("管理员解冻账户，operatorId={}, accountNo={}", operatorId, request.getAccountNo());
        accountService.unfreezeAccount(operatorId, request);
        return ApiResponse.success();
    }

    /**
     * 查询账户详情
     *
     * @param accountNo 账户编号
     * @return 账户详情
     */
    @GetMapping("/{accountNo}")
    @Operation(summary = "查询账户详情", description = "管理员根据账户编号查询账户完整信息")
    public ApiResponse<AccountProfileVO> getAccountDetail(@PathVariable String accountNo) {
        log.info("管理员查询账户详情，accountNo={}", accountNo);
        AccountProfileVO result = accountService.getAccountDetail(accountNo);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询账户列表
     *
     * @param query 分页查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询账户列表", description = "管理员查看所有注册账户的分页列表")
    public ApiResponse<PageResult<AccountProfileVO>> listAccounts(@Valid PageQuery query) {
        log.info("管理员查询账户列表，pageNo={}, pageSize={}", query.getPageNo(), query.getPageSize());
        PageResult<AccountProfileVO> result = accountService.listAccounts(query);
        return ApiResponse.success(result);
    }

    @PostMapping("/create-platform")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "创建平台管理员账户", description = "由超级管理员后台直接添加平台管理员，无需短信验证码")
    public ApiResponse<Void> createPlatformAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("创建平台管理员账户，operatorId={}, mobile={}", operatorId, request.getMobile());
        accountService.createPlatformAccount(operatorId, request);
        return ApiResponse.success();
    }

    @PostMapping("/create-regulator")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "创建监管员账户", description = "由超级管理员后台直接添加监管员，无需短信验证码")
    public ApiResponse<Void> createRegulatorAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("创建监管员账户，operatorId={}, mobile={}", operatorId, request.getMobile());
        accountService.createRegulatorAccount(operatorId, request);
        return ApiResponse.success();
    }
}
