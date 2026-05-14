package com.lifechain.trade.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.trade.dto.CreateLicenseTemplateRequest;
import com.lifechain.trade.dto.LicenseDetailVO;
import com.lifechain.trade.dto.LicenseTemplateVO;
import com.lifechain.trade.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
@Tag(name = "授权许可", description = "授权记录查询与模板管理")
public class LicenseController {

    private final LicenseService licenseService;

    @PostMapping("/templates")
    @Operation(summary = "创建授权模板", description = "创建新的授权许可模板，仅创作者或管理员可操作")
    @Idempotent
    @PreAuthorize("hasAnyRole('CREATOR','PLATFORM_ADMIN')")
    public ApiResponse<LicenseTemplateVO> createTemplate(@Valid @RequestBody CreateLicenseTemplateRequest request) {
        return ApiResponse.success(licenseService.createTemplate(request));
    }

    @GetMapping("/templates")
    @Operation(summary = "查询授权模板列表", description = "分页查询所有授权许可模板，登录用户可访问")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<LicenseTemplateVO>> listTemplates(@Valid PageQuery query) {
        PageResult<LicenseTemplateVO> result = licenseService.listTemplates(query);
        return ApiResponse.success(result);
    }

    @GetMapping("/templates/{templateCode}")
    @Operation(summary = "查询授权模板详情", description = "根据模板编码查询授权模板详细信息，登录用户可访问")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LicenseTemplateVO> getTemplateDetail(@PathVariable String templateCode) {
        LicenseTemplateVO result = licenseService.getTemplateDetail(templateCode);
        return ApiResponse.success(result);
    }

    @GetMapping("/{licenseNo}")
    @Operation(summary = "查询授权详情", description = "根据授权编号查询授权许可完整信息")
    public ApiResponse<LicenseDetailVO> getLicenseDetail(@PathVariable String licenseNo) {
        LicenseDetailVO result = licenseService.getLicenseDetail(licenseNo, UserContext.getUserId());
        return ApiResponse.success(result);
    }

    /**
     * 查询我的授权列表
     * <p>
     * 查询当前用户作为被授权方的所有授权记录，按创建时间倒序分页。
     * </p>
     *
     * @param query 分页参数
     * @return 分页授权列表
     */
    @GetMapping("/mine")
    @Operation(summary = "查询我的授权", description = "分页查询我作为被授权方的授权记录")
    public ApiResponse<PageResult<LicenseDetailVO>> listMyLicenses(@Valid PageQuery query) {
        Long accountId = UserContext.getUserId();
        PageResult<LicenseDetailVO> result = licenseService.listMyLicenses(accountId, query);
        return ApiResponse.success(result);
    }
}
