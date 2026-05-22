package com.lifechain.app.controller;

import com.lifechain.app.service.WorkPreviewService;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.work.dto.PreviewUrlVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 作品文件预览控制器
 * <p>
 * 提供登录用户和市场公开两种预览 URL 获取接口，
 * 根据访问者身份返回不同权限级别的签名 URL。
 * </p>
 *
 * @author LifeChain
 */
@Tag(name = "作品预览", description = "作品文件预览URL获取")
@RestController
@RequiredArgsConstructor
public class WorkPreviewController {

    private final WorkPreviewService workPreviewService;

    /**
     * 获取作品文件预览URL（需登录）
     *
     * @param workNo 作品编号
     * @param fileId 文件ID
     * @return 包含签名预览URL和访问级别的响应
     */
    @GetMapping("/api/works/{workNo}/preview-url")
    @Operation(summary = "获取作品文件预览URL（需登录）")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PreviewUrlVO> getPreviewUrl(
            @PathVariable String workNo,
            @RequestParam Long fileId) {
        Long accountId = UserContext.getUserId();
        return ApiResponse.success(workPreviewService.getPreviewUrl(workNo, fileId, accountId));
    }

    /**
     * 获取市场作品文件预览URL（公开访问）
     *
     * @param workNo 作品编号
     * @param fileId 文件ID
     * @return 包含有限预览URL的响应
     */
    @GetMapping("/api/market/works/{workNo}/preview-url")
    @Operation(summary = "获取市场作品文件预览URL（公开）")
    public ApiResponse<PreviewUrlVO> getMarketPreviewUrl(
            @PathVariable String workNo,
            @RequestParam Long fileId) {
        return ApiResponse.success(workPreviewService.getMarketPreviewUrl(workNo, fileId));
    }
}
