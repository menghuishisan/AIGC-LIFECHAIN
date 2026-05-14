package com.lifechain.app.controller;

import com.lifechain.app.dto.UpsertSysConfigRequest;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.infra.config.SysConfigService;
import com.lifechain.infra.config.SysConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统配置", description = "配置中心管理")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping("/api/admin/configs")
    @Operation(summary = "查询所有配置")
    public ApiResponse<List<SysConfigVO>> listAll(
            @RequestParam(required = false) String configType) {
        if (configType != null) {
            return ApiResponse.success(sysConfigService.listByType(configType));
        }
        return ApiResponse.success(sysConfigService.listAll());
    }

    @GetMapping("/api/admin/configs/{configKey}")
    @Operation(summary = "查询单个配置")
    public ApiResponse<String> getConfig(@PathVariable String configKey) {
        return ApiResponse.success(sysConfigService.getConfigValue(configKey));
    }

    @PostMapping("/api/admin/configs")
    @Operation(summary = "创建或更新配置")
    @com.lifechain.common.annotation.Idempotent(key = "#request.requestId")
    public ApiResponse<SysConfigVO> upsertConfig(@Valid @RequestBody UpsertSysConfigRequest request) {
        return ApiResponse.success(sysConfigService.upsertConfig(
                request.getConfigKey(), request.getConfigValue(),
                request.getConfigType(), request.getDescription()));
    }

    @DeleteMapping("/api/admin/configs/{configKey}")
    @Operation(summary = "删除配置")
    @com.lifechain.common.annotation.Idempotent
    public ApiResponse<Void> deleteConfig(@PathVariable String configKey,
                                          @RequestParam @jakarta.validation.constraints.NotBlank String requestId) {
        sysConfigService.deleteConfig(configKey);
        return ApiResponse.success();
    }
}
