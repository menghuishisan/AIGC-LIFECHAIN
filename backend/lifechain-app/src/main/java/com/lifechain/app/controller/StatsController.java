package com.lifechain.app.controller;

import com.lifechain.app.dto.DistributionVO;
import com.lifechain.app.dto.StatsCountVO;
import com.lifechain.app.dto.StatsOverviewVO;
import com.lifechain.app.dto.TrendPointVO;
import com.lifechain.app.service.StatsService;
import com.lifechain.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "平台统计", description = "管理后台统计概览接口")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "平台概览统计", description = "获取平台核心业务统计数据")
    @GetMapping("/overview")
    public ApiResponse<StatsOverviewVO> overview() {
        return ApiResponse.success(statsService.getOverview());
    }

    @Operation(summary = "确权趋势统计", description = "获取近N天确权申请数量时间序列")
    @GetMapping("/claims/trend")
    public ApiResponse<List<TrendPointVO>> claimsTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(statsService.getClaimsTrend(days));
    }

    @Operation(summary = "订单趋势统计", description = "获取近N天订单数量时间序列")
    @GetMapping("/orders/trend")
    public ApiResponse<List<TrendPointVO>> ordersTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(statsService.getOrdersTrend(days));
    }

    @Operation(summary = "结算统计", description = "获取结算记录统计数据")
    @GetMapping("/settlements")
    public ApiResponse<StatsCountVO> settlements() {
        return ApiResponse.success(statsService.getSettlementsCount());
    }

    @Operation(summary = "验真统计", description = "获取验真查询统计数据")
    @GetMapping("/verify")
    public ApiResponse<StatsCountVO> verify() {
        return ApiResponse.success(statsService.getVerifyCount());
    }

    @Operation(summary = "风险统计", description = "获取风险事件统计数据")
    @GetMapping("/risk")
    public ApiResponse<StatsCountVO> risk() {
        return ApiResponse.success(statsService.getRiskCount());
    }

    @Operation(summary = "分布统计", description = "获取作品、订单、账户、风险等分布统计")
    @GetMapping("/distribution")
    public ApiResponse<DistributionVO> distribution() {
        return ApiResponse.success(statsService.getDistribution());
    }
}
