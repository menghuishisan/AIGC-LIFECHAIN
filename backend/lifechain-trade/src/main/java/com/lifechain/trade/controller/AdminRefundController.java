package com.lifechain.trade.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.trade.dto.RefundDetailVO;
import com.lifechain.trade.dto.RefundProcessRequest;
import com.lifechain.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员退款处理控制器
 * <p>
 * 管理员审批退款申请，支持通过或驳回。
 * 通过后系统自动调用支付渠道退款接口。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Tag(name = "退款管理（管理员）", description = "管理员审批退款申请")
public class AdminRefundController {

    private final OrderService orderService;

    /**
     * 分页查询退款列表
     *
     * @param status 退款状态（可选）
     * @param query  分页参数
     * @return 分页退款列表
     */
    @GetMapping
    @Operation(summary = "查询退款列表", description = "管理员分页查询退款记录，支持按状态筛选")
    public ApiResponse<PageResult<RefundDetailVO>> listRefunds(
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        log.info("管理员查询退款列表，status={}", status);
        PageResult<RefundDetailVO> result = orderService.listRefunds(status, query);
        return ApiResponse.success(result);
    }

    /**
     * 查询退款详情
     *
     * @param refundNo 退款编号
     * @return 退款详情
     */
    @GetMapping("/{refundNo}")
    @Operation(summary = "查询退款详情", description = "管理员根据退款编号查询退款详细信息")
    public ApiResponse<RefundDetailVO> getRefundDetail(@PathVariable String refundNo) {
        log.info("管理员查询退款详情，refundNo={}", refundNo);
        RefundDetailVO result = orderService.getRefundDetail(refundNo);
        return ApiResponse.success(result);
    }

    /**
     * 处理退款审批
     * <p>
     * 管理员审批退款申请，传入 APPROVE 通过或 REJECT 驳回。
     * 通过后自动触发支付渠道退款。
     * </p>
     *
     * @param request 退款处理请求
     * @return 成功响应
     */
    @PostMapping("/process")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "处理退款", description = "管理员审批退款申请，通过后自动触发渠道退款")
    public ApiResponse<Void> processRefund(@Valid @RequestBody RefundProcessRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("处理退款审批，operatorId={}, refundNo={}, action={}",
                operatorId, request.getRefundNo(), request.getAction());
        orderService.processRefund(operatorId, request);
        return ApiResponse.success();
    }
}
