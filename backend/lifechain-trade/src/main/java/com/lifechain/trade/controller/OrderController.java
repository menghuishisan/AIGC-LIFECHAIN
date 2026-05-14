package com.lifechain.trade.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.trade.dto.*;
import com.lifechain.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 * <p>
 * 处理交易订单的创建、查询、支付、取消和退款申请等请求。
 * 所有写操作通过幂等注解保障重复请求安全。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "交易订单", description = "订单创建、支付、取消与退款")
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建交易订单
     *
     * @param request 创建订单请求
     * @return 订单详情
     */
    @PostMapping
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "创建订单", description = "买方选择上架作品并下单")
    public ApiResponse<OrderDetailVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long accountId = UserContext.getUserId();
        log.info("创建订单，accountId={}, listingNo={}", accountId, request.getListingNo());
        OrderDetailVO result = orderService.createOrder(accountId, request);
        return ApiResponse.success(result);
    }

    /**
     * 查询订单详情
     *
     * @param orderNo 订单编号
     * @return 订单详情
     */
    @GetMapping("/{orderNo}")
    @Operation(summary = "查询订单详情", description = "根据订单编号查询订单完整信息")
    public ApiResponse<OrderDetailVO> getOrderDetail(@PathVariable String orderNo) {
        Long accountId = UserContext.getUserId();
        OrderDetailVO result = orderService.getOrderDetail(orderNo, accountId);
        return ApiResponse.success(result);
    }

    /**
     * 查询我的订单列表
     * <p>
     * 支持按角色（BUYER/CREATOR）和订单状态筛选。
     * </p>
     *
     * @param role   角色（BUYER-买方/CREATOR-创作者）
     * @param status 订单状态（可选）
     * @param query  分页参数
     * @return 分页订单列表
     */
    @GetMapping
    @Operation(summary = "查询我的订单列表", description = "分页查询我的订单，支持按角色和状态筛选")
    public ApiResponse<PageResult<OrderListVO>> listMyOrders(
            @RequestParam(required = false, defaultValue = "BUYER") String role,
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        Long accountId = UserContext.getUserId();
        PageResult<OrderListVO> result = orderService.listMyOrders(accountId, role, status, query);
        return ApiResponse.success(result);
    }

    /**
     * 发起支付
     *
     * @param orderNo 订单编号
     * @param request 支付请求参数
     * @return 支付结果（含前端调起支付所需参数）
     */
    @PostMapping("/{orderNo}/pay")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "发起支付", description = "对已创建的订单发起支付")
    public ApiResponse<PayResultVO> payOrder(@PathVariable String orderNo,
                                             @Valid @RequestBody PayRequest request) {
        Long accountId = UserContext.getUserId();
        request.setOrderNo(orderNo);
        log.info("发起支付，accountId={}, orderNo={}", accountId, orderNo);
        PayResultVO result = orderService.payOrder(accountId, request);
        return ApiResponse.success(result);
    }

    /**
     * 取消订单
     *
     * @param orderNo 订单编号
     * @return 成功响应
     */
    @DeleteMapping("/{orderNo}")
    @Operation(summary = "取消订单", description = "买方取消未支付的订单")
    @Idempotent
    public ApiResponse<Void> cancelOrder(@PathVariable String orderNo,
                                          @RequestParam @jakarta.validation.constraints.NotBlank String requestId) {
        Long accountId = UserContext.getUserId();
        log.info("取消订单，accountId={}, orderNo={}", accountId, orderNo);
        orderService.cancelOrder(accountId, orderNo);
        return ApiResponse.success();
    }

    /**
     * 申请退款
     *
     * @param orderNo 订单编号
     * @param request 退款申请参数
     * @return 成功响应
     */
    @PostMapping("/{orderNo}/refund")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "申请退款", description = "买方对已支付订单申请退款")
    public ApiResponse<Void> applyRefund(@PathVariable String orderNo,
                                         @Valid @RequestBody RefundApplyRequest request) {
        Long accountId = UserContext.getUserId();
        request.setOrderNo(orderNo);
        log.info("申请退款，accountId={}, orderNo={}", accountId, orderNo);
        orderService.applyRefund(accountId, request);
        return ApiResponse.success();
    }
}
