package com.lifechain.app.controller;

import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.infra.attachment.AttachmentService;
import com.lifechain.infra.attachment.AttachmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "附件管理", description = "系统附件查询（仅管理员）")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/api/admin/attachments")
    @Operation(summary = "查询附件列表", description = "按业务类型和业务编号查询关联附件")
    public ApiResponse<PageResult<AttachmentVO>> listAttachments(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizNo,
            @Valid PageQuery query) {
        return ApiResponse.success(attachmentService.listAttachments(
                bizType, bizNo, query.getPageNo(), query.getPageSize()));
    }
}
