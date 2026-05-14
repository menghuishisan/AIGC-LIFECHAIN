package com.lifechain.work.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.work.dto.ClaimDetailVO;
import com.lifechain.work.dto.ClaimSubmitRequest;
import com.lifechain.work.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 确权控制器
 * <p>
 * 处理确权申请提交、确权详情查询和确权列表查询等请求。
 * 所有接口需要登录认证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "确权管理", description = "确权申请提交与查询")
public class ClaimController {

    private final ClaimService claimService;

    /**
     * 提交确权申请
     *
     * @param request 确权申请请求
     * @return 确权详情
     */
    @PostMapping("/submit")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "提交确权申请", description = "对已完成特征提取的作品提交确权申请")
    public ApiResponse<ClaimDetailVO> submitClaim(@Valid @RequestBody ClaimSubmitRequest request) {
        Long accountId = UserContext.getUserId();
        log.info("提交确权申请，accountId={}, workNo={}", accountId, request.getWorkNo());
        ClaimDetailVO result = claimService.submitClaim(accountId, request);
        return ApiResponse.success(result);
    }

    /**
     * 查询确权详情
     *
     * @param claimNo 确权编号
     * @return 确权详情
     */
    @GetMapping("/{claimNo}")
    @Operation(summary = "确权详情", description = "查询确权申请的完整详情信息")
    public ApiResponse<ClaimDetailVO> getClaimDetail(@PathVariable String claimNo) {
        ClaimDetailVO result = claimService.getClaimDetail(claimNo, UserContext.getUserId());
        return ApiResponse.success(result);
    }

    /**
     * 查询确权申请的链上回执信息
     *
     * @param claimNo 确权编号
     * @return 确权详情（含链上信息）
     */
    @GetMapping("/{claimNo}/chain-receipt")
    @Operation(summary = "链上回执", description = "查询确权申请的链上交易回执信息")
    public ApiResponse<ClaimDetailVO> getChainReceipt(@PathVariable String claimNo) {
        ClaimDetailVO result = claimService.getClaimChainReceipt(claimNo, UserContext.getUserId());
        return ApiResponse.success(result);
    }

    /**
     * 查询我的确权列表
     *
     * @param status 状态筛选（可选）
     * @param query  分页参数
     * @return 分页确权列表
     */
    @GetMapping
    @Operation(summary = "我的确权列表", description = "查询当前登录用户的确权申请列表")
    public ApiResponse<PageResult<ClaimDetailVO>> listClaims(@RequestParam(required = false) String status,
                                                             @Valid PageQuery query) {
        Long accountId = UserContext.getUserId();
        PageResult<ClaimDetailVO> result = claimService.listClaims(accountId, status, query);
        return ApiResponse.success(result);
    }
}
