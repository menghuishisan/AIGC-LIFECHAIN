package com.lifechain.app.controller;

import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.infra.notification.MessageNoticeVO;
import com.lifechain.infra.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "消息通知", description = "站内消息通知查询与已读标记")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NoticeController {

    private final NotificationService notificationService;

    @GetMapping("/api/notices")
    @Operation(summary = "查询我的通知列表", description = "分页查询当前用户的站内通知")
    public ApiResponse<PageResult<MessageNoticeVO>> listNotices(
            @RequestParam(required = false) String noticeType,
            @RequestParam(required = false) Integer readFlag,
            @Valid PageQuery query) {
        Long accountId = UserContext.getUserId();
        return ApiResponse.success(notificationService.listNotices(
                accountId, noticeType, readFlag, query.getPageNo(), query.getPageSize()));
    }

    @PostMapping("/api/notices/{noticeNo}/read")
    @Operation(summary = "标记通知已读", description = "将指定通知标记为已读状态")
    @com.lifechain.common.annotation.Idempotent
    public ApiResponse<Void> markRead(@PathVariable String noticeNo,
                                       @RequestParam @jakarta.validation.constraints.NotBlank String requestId) {
        Long accountId = UserContext.getUserId();
        notificationService.markRead(accountId, noticeNo);
        return ApiResponse.success();
    }

    @GetMapping("/api/notices/{noticeNo}")
    @Operation(summary = "查询通知详情", description = "根据通知编号查询通知详细信息")
    public ApiResponse<MessageNoticeVO> getNoticeDetail(@PathVariable String noticeNo) {
        Long accountId = UserContext.getUserId();
        MessageNoticeVO result = notificationService.getNoticeDetail(accountId, noticeNo);
        return ApiResponse.success(result);
    }
}
