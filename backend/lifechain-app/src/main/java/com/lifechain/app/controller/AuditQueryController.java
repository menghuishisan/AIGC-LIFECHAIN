package com.lifechain.app.controller;

import com.lifechain.auth.audit.AuditLogVO;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.StatusHistoryVO;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "审计与状态历史", description = "审计日志与状态变更历史查询")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditQueryController {

    private final AuditService auditService;

    @GetMapping("/api/status-history")
    @Operation(summary = "查询状态变更历史", description = "按业务类型和业务编号查询状态变更历史")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','REGULATOR')")
    public ApiResponse<PageResult<StatusHistoryVO>> listStatusHistory(
            @RequestParam String bizType,
            @RequestParam String bizNo,
            @Valid PageQuery query) {
        return ApiResponse.success(auditService.listStatusHistory(bizType, bizNo, query));
    }

    @GetMapping("/api/admin/audit/logs")
    @Operation(summary = "查询审计日志", description = "分页查询审计日志，支持按目标类型和动作筛选")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<PageResult<AuditLogVO>> listAuditLogs(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String action,
            @Valid PageQuery query) {
        return ApiResponse.success(auditService.listAuditLogs(targetType, action, query));
    }
}
