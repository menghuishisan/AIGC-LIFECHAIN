package com.lifechain.app.controller;

import com.lifechain.app.dto.RegulatorSearchResultVO;
import com.lifechain.app.service.RegulatorSearchService;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.common.context.UserContext;
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

@Tag(name = "监管搜索", description = "监管方综合跨对象搜索接口")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('REGULATOR','PLATFORM_ADMIN')")
public class RegulatorSearchController {

    private final RegulatorSearchService regulatorSearchService;
    private final AuditService auditService;

    @GetMapping("/api/regulator/search")
    @Operation(summary = "监管综合搜索", description = "按关键词跨对象搜索，支持按目标类型过滤")
    public ApiResponse<PageResult<RegulatorSearchResultVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String targetType,
            @Valid PageQuery query) {

        PageResult<RegulatorSearchResultVO> result = regulatorSearchService.search(keyword, targetType, query);
        writeSearchAudit(keyword, targetType, result.getTotal());
        return ApiResponse.success(result);
    }

    private void writeSearchAudit(String keyword, String targetType, long total) {
        auditService.writeAuditLog("REGULATOR_SEARCH", null, null,
                "SEARCH", "keyword=" + keyword + ", targetType=" + targetType + ", results=" + total,
                UserContext.getUserId(), "REGULATOR", null, "SUCCESS", null);
    }
}
