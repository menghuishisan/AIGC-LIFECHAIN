package com.lifechain.trade.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.trade.dto.CreateListingRequest;
import com.lifechain.trade.dto.ListingDetailVO;
import com.lifechain.trade.dto.ListingReviewRequest;
import com.lifechain.trade.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 上架控制器
 * <p>
 * 处理作品上架、审核、市场列表查询及下架等请求。
 * 创建上架和下架需创作者本人操作，审核需管理员角色。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "作品上架", description = "作品上架、审核、市场浏览与下架")
public class ListingController {

    private final ListingService listingService;

    /**
     * 创建上架申请
     * <p>
     * 创作者将已确权作品提交上架申请，创建后状态为待审核。
     * </p>
     *
     * @param workNo  作品编号（路径参数）
     * @param request 上架请求参数
     * @return 上架详情
     */
    @PostMapping("/api/works/{workNo}/listing")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "创建上架申请", description = "创作者将已确权作品提交到交易市场")
    public ApiResponse<ListingDetailVO> createListing(@PathVariable String workNo,
                                                      @Valid @RequestBody CreateListingRequest request) {
        Long accountId = UserContext.getUserId();
        request.setWorkNo(workNo);
        log.info("创建上架申请，accountId={}, workNo={}", accountId, workNo);
        ListingDetailVO result = listingService.createListing(accountId, request);
        return ApiResponse.success(result);
    }

    /**
     * 审核上架申请（管理员）
     *
     * @param request 审核请求参数
     * @return 成功响应
     */
    @PostMapping("/api/admin/listings/review")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "审核上架申请", description = "管理员审核上架申请，通过或驳回")
    public ApiResponse<Void> reviewListing(@Valid @RequestBody ListingReviewRequest request) {
        Long reviewerId = UserContext.getUserId();
        log.info("审核上架申请，reviewerId={}, listingNo={}", reviewerId, request.getListingNo());
        listingService.reviewListing(reviewerId, request);
        return ApiResponse.success();
    }

    /**
     * 查询上架详情
     *
     * @param listingNo 上架编号
     * @return 上架详情
     */
    @GetMapping("/api/listings/{listingNo}")
    @Operation(summary = "查询上架详情", description = "根据上架编号查询上架信息")
    public ApiResponse<ListingDetailVO> getListingDetail(@PathVariable String listingNo) {
        ListingDetailVO result = listingService.getListingDetail(listingNo);
        return ApiResponse.success(result);
    }

    /**
     * 查询我的上架列表（创作者）
     *
     * @param status 上架状态（可选）
     * @param query  分页参数
     * @return 分页上架列表
     */
    @GetMapping("/api/listings/mine")
    @Operation(summary = "查询我的上架列表", description = "创作者查询自己的上架作品列表")
    public ApiResponse<PageResult<ListingDetailVO>> listMyListings(
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        Long accountId = UserContext.getUserId();
        log.info("查询我的上架列表，accountId={}, status={}", accountId, status);
        PageResult<ListingDetailVO> result = listingService.listMyListings(accountId, status, query);
        return ApiResponse.success(result);
    }

    /**
     * 管理员查询上架审核列表
     *
     * @param reviewStatus 审核状态（可选）
     * @param query        分页参数
     * @return 分页上架列表
     */
    @GetMapping("/api/admin/listings")
    @Operation(summary = "管理员查询上架列表", description = "管理员分页查询上架记录，支持按状态筛选")
    public ApiResponse<PageResult<ListingDetailVO>> listAdminListings(
            @RequestParam(required = false) String reviewStatus,
            @Valid PageQuery query) {
        log.info("管理员查询上架列表，reviewStatus={}", reviewStatus);
        PageResult<ListingDetailVO> result = listingService.listAdminListings(reviewStatus, query);
        return ApiResponse.success(result);
    }

    /**
     * 下架作品
     * <p>
     * 创作者主动下架已上架的作品。
     * </p>
     *
     * @param listingNo 上架编号
     * @return 成功响应
     */
    @DeleteMapping("/api/listings/{listingNo}")
    @Operation(summary = "下架作品", description = "创作者主动下架已上架的作品")
    @Idempotent
    public ApiResponse<Void> unlistWork(@PathVariable String listingNo,
                                         @RequestParam @jakarta.validation.constraints.NotBlank String requestId) {
        Long accountId = UserContext.getUserId();
        log.info("下架作品，accountId={}, listingNo={}", accountId, listingNo);
        listingService.unlistWork(accountId, listingNo);
        return ApiResponse.success();
    }
}
