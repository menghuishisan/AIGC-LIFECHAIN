package com.lifechain.regulator.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.CreateReportRequest;
import com.lifechain.regulator.dto.HandleReportRequest;
import com.lifechain.regulator.dto.ReportVO;
import com.lifechain.regulator.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 监管报告管理控制器
 * <p>
 * 提供监管报告的创建、处理和查询接口，仅管理员可操作。
 * 已完成的报告通过摘要哈希上链存证，确保报告的不可篡改性。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/regulator/reports")
@RequiredArgsConstructor
@Tag(name = "监管报告管理", description = "监管报告创建、生成与查询")
public class ReportController {

    private final ReportService reportService;

    /**
     * 创建监管报告
     *
     * @param request 创建请求
     * @return 监管报告详情
     */
    @PostMapping
    @Operation(summary = "创建监管报告", description = "新增监管报告草稿")
    @Idempotent
    public ApiResponse<ReportVO> createReport(@Valid @RequestBody CreateReportRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("创建监管报告，operatorId={}, reportType={}", operatorId, request.getReportType());
        ReportVO result = reportService.createReport(operatorId, request);
        return ApiResponse.success(result);
    }

    @PostMapping("/generate")
    @Operation(summary = "生成监管报告", description = "创建并触发生成监管报告")
    @Idempotent
    public ApiResponse<ReportVO> generateReport(@Valid @RequestBody CreateReportRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("生成监管报告，operatorId={}, reportType={}", operatorId, request.getReportType());
        ReportVO created = reportService.createReport(operatorId, request);
        HandleReportRequest handleRequest = new HandleReportRequest();
        handleRequest.setReportNo(created.getReportNo());
        handleRequest.setAction("GENERATE");
        ReportVO result = reportService.handleReport(operatorId, handleRequest);
        return ApiResponse.success(result);
    }

    /**
     * 处理监管报告
     *
     * @param reportNo 报告编号
     * @param request  处理请求
     * @return 更新后的监管报告详情
     */
    @PostMapping("/{reportNo}/handle")
    @Operation(summary = "处理监管报告", description = "触发生成、标记完成或标记失败")
    @Idempotent
    public ApiResponse<ReportVO> handleReport(@PathVariable String reportNo,
                                               @Valid @RequestBody HandleReportRequest request) {
        Long operatorId = UserContext.getUserId();
        request.setReportNo(reportNo);
        log.info("处理监管报告，operatorId={}, reportNo={}, action={}", operatorId, reportNo, request.getAction());
        ReportVO result = reportService.handleReport(operatorId, request);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询监管报告列表
     *
     * @param reportType 报告类型（可选）
     * @param status     状态（可选）
     * @param query      分页参数
     * @return 分页监管报告列表
     */
    @GetMapping
    @Operation(summary = "查询监管报告列表", description = "分页查询监管报告，支持按类型和状态筛选")
    public ApiResponse<PageResult<ReportVO>> listReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        PageResult<ReportVO> result = reportService.listReports(reportType, status, query);
        return ApiResponse.success(result);
    }

    /**
     * 查询监管报告详情
     *
     * @param reportNo 报告编号
     * @return 监管报告详情
     */
    @GetMapping("/{reportNo}")
    @Operation(summary = "查询监管报告详情", description = "根据报告编号查询完整监管报告信息")
    public ApiResponse<ReportVO> getDetail(@PathVariable String reportNo) {
        ReportVO result = reportService.getByReportNo(reportNo);
        return ApiResponse.success(result);
    }
}
