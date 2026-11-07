package com.lifechain.app.controller;

import com.lifechain.app.dto.IncomeItemVO;
import com.lifechain.app.dto.IncomeSummaryVO;
import com.lifechain.settlement.dto.IncomeTrendPoint;
import java.util.List;
import com.lifechain.app.service.IncomeService;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收益查询", description = "创作者收益汇总与明细查询")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {

    private final IncomeService incomeService;

    @GetMapping("/api/income/trend")
    @Operation(summary = "收益趋势", description = "最近三十个 UTC 自然日的已确认净收益，金额单位为分")
    public ApiResponse<List<IncomeTrendPoint>> incomeTrend() {
        return ApiResponse.success(incomeService.getIncomeTrend(UserContext.getUserId()));
    }

    @GetMapping("/api/income/summary")
    @Operation(summary = "收益汇总", description = "查询当前用户的收益汇总数据")
    public ApiResponse<IncomeSummaryVO> incomeSummary() {
        return ApiResponse.success(incomeService.getIncomeSummary(UserContext.getUserId()));
    }

    @GetMapping("/api/income/details")
    @Operation(summary = "收益明细", description = "分页查询当前用户的收益明细列表")
    public ApiResponse<PageResult<IncomeItemVO>> incomeDetails(@Valid PageQuery query) {
        return ApiResponse.success(incomeService.listIncomeDetails(UserContext.getUserId(), query));
    }
}
