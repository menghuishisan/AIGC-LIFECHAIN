package com.lifechain.app.controller;

import com.lifechain.app.dto.TraceEventVO;
import com.lifechain.app.service.TraceQueryService;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.enums.ViewerRole;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.work.entity.WorkEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 追踪事件控制器
 * <p>
 * 提供业务实体的全链路追溯查询接口，支持按业务类型和业务编号
 * 查询该实体的所有生命周期事件。
 * 普通用户仅可查看自己相关的trace，管理员和监管员可查看全部。
 * </p>
 */
@Tag(name = "追踪溯源", description = "业务实体生命周期追溯接口")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TraceController {

    private final TraceQueryService traceQueryService;

    @Operation(summary = "查询作品追踪事件", description = "根据作品编号查询全链路追踪事件")
    @GetMapping("/api/traces/work/{workNo}")
    public ApiResponse<List<TraceEventVO>> queryWorkTraceEvents(
            @Parameter(description = "作品编号") @PathVariable String workNo) {
        ViewerRole role = FieldVisibilityUtil.resolveViewerRole();
        if (role == ViewerRole.USER || role == ViewerRole.PUBLIC) {
            WorkEntity work = traceQueryService.getWorkByNo(workNo);
            if (work == null) {
                throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
            }
            Long userId = UserContext.getUserId();
            if (!work.getCreatorAccountId().equals(userId)) {
                throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该作品追踪事件");
            }
        }
        return ApiResponse.success(traceQueryService.queryTraceEvents("WORK", workNo));
    }

    @Operation(summary = "查询订单追踪事件", description = "根据订单编号查询全链路追踪事件")
    @GetMapping("/api/traces/order/{orderNo}")
    public ApiResponse<List<TraceEventVO>> queryOrderTraceEvents(
            @Parameter(description = "订单编号") @PathVariable String orderNo) {
        ViewerRole role = FieldVisibilityUtil.resolveViewerRole();
        if (role == ViewerRole.USER || role == ViewerRole.PUBLIC) {
            TradeOrderEntity order = traceQueryService.getOrderByNo(orderNo);
            if (order == null) {
                throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
            }
            Long userId = UserContext.getUserId();
            if (!order.getBuyerAccountId().equals(userId)
                    && !order.getCreatorAccountId().equals(userId)) {
                throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该订单追踪事件");
            }
        }
        return ApiResponse.success(traceQueryService.queryTraceEvents("ORDER", orderNo));
    }
}
