package com.lifechain.work.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.work.dto.ClaimListVO;
import com.lifechain.work.dto.ClaimReviewRequest;
import com.lifechain.work.service.ClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 确权审核管理控制器（管理员）
 * <p>
 * 处理管理员对确权申请的审核操作，需要管理员权限。
 * 审核通过后系统自动触发链上确权流程。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/claims")
@RequiredArgsConstructor
@Tag(name = "确权审核管理", description = "管理员确权审核")
public class AdminClaimController {

    private final ClaimService claimService;

    /**
     * 查询全部确权申请列表（管理员）
     *
     * @param status 确权状态筛选（可选）
     * @param query  分页参数
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "确权审核列表", description = "管理员分页查询全部确权申请，可按状态筛选")
    public ApiResponse<PageResult<ClaimListVO>> listClaims(
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        log.info("管理员查询确权列表，status={}, pageNo={}, pageSize={}",
                status, query.getPageNo(), query.getPageSize());
        PageResult<ClaimListVO> result = claimService.listAllClaims(status, query);
        return ApiResponse.success(result);
    }

    /**
     * 审核确权申请
     *
     * @param request 审核请求
     * @return 成功响应
     */
    @PostMapping("/review")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "审核确权申请", description = "管理员对确权申请进行审核，通过后自动触发上链")
    public ApiResponse<Void> reviewClaim(@Valid @RequestBody ClaimReviewRequest request) {
        Long reviewerId = UserContext.getUserId();
        log.info("审核确权申请，reviewerId={}, claimNo={}", reviewerId, request.getClaimNo());
        claimService.reviewClaim(reviewerId, request);
        return ApiResponse.success();
    }
}
