package com.lifechain.app.controller;

import com.lifechain.app.dto.ScreenRealtimeVO;
import com.lifechain.app.service.ScreenService;
import com.lifechain.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "大屏数据", description = "实时大屏数据接口")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ScreenController {

    private final ScreenService screenService;

    @GetMapping("/api/screen/realtime")
    @Operation(summary = "大屏实时数据", description = "获取平台实时运营数据，供大屏展示")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ApiResponse<ScreenRealtimeVO> realtimeScreen() {
        return ApiResponse.success(screenService.getRealtimeData());
    }
}
