package com.lifechain.trade.controller;

import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.trade.dto.AdminOrderListVO;
import com.lifechain.trade.dto.AdminOrderQuery;
import com.lifechain.trade.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员订单管理控制器
 * <p>
 * 提供平台管理员全量订单列表查询能力，支持多维度筛选。
 * 不改变用户侧 /api/orders 的"我的订单"语义。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "订单管理（管理员）", description = "管理员全量订单列表查询")
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * 分页查询全量订单列表
     *
     * @param adminQuery 管理员筛选条件
     * @param pageQuery  分页参数
     * @return 分页订单列表
     */
    @GetMapping
    @Operation(summary = "查询全量订单列表", description = "管理员按条件分页查询所有订单")
    public ApiResponse<PageResult<AdminOrderListVO>> listAdminOrders(
            @Valid AdminOrderQuery adminQuery,
            @Valid PageQuery pageQuery) {
        log.info("管理员查询订单列表，orderNo={}, orderStatus={}", adminQuery.getOrderNo(), adminQuery.getOrderStatus());
        PageResult<AdminOrderListVO> result = orderService.listAdminOrders(adminQuery, pageQuery);
        return ApiResponse.success(result);
    }
}
