package com.lifechain.settlement.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.settlement.dto.RetrySettlementRequest;
import com.lifechain.settlement.dto.ReverseSettlementRequest;
import com.lifechain.settlement.dto.ReverseSettlementVO;
import com.lifechain.settlement.dto.SettlementRecordVO;
import com.lifechain.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 结算记录管理控制器
 * <p>
 * 提供结算记录查询、失败重试和逆分账等管理接口，仅管理员可操作。
 * 结算由订单支付完成后自动触发，此处提供管理侧的查询与干预能力。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "结算记录管理", description = "结算查询、失败重试与逆分账")
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping("/api/admin/settlements")
    @Operation(summary = "分页查询结算列表", description = "管理员分页查询结算记录，支持按状态筛选")
    public ApiResponse<PageResult<SettlementRecordVO>> listSettlements(
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        log.info("管理员查询结算列表，status={}", status);
        PageResult<SettlementRecordVO> result = settlementService.listSettlements(status, query);
        return ApiResponse.success(result);
    }

    @GetMapping("/api/admin/settlements/{settleNo}")
    @Operation(summary = "查询结算详情", description = "根据结算编号查询包含明细的完整结算信息")
    public ApiResponse<SettlementRecordVO> getSettlement(@PathVariable String settleNo) {
        SettlementRecordVO result = settlementService.getSettlementBySettleNo(settleNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/api/orders/{orderNo}/settlement")
    @Operation(summary = "按订单查询结算", description = "根据订单编号查询关联的结算记录，仅管理员或监管方可用")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'REGULATOR')")
    public ApiResponse<SettlementRecordVO> getByOrderNo(@PathVariable String orderNo) {
        SettlementRecordVO result = settlementService.getSettlementByOrderNo(orderNo);
        return ApiResponse.success(result);
    }

    @PostMapping("/api/admin/settlements/retry")
    @Operation(summary = "重试结算", description = "对失败状态的结算重新提交链上存证")
    @Idempotent
    public ApiResponse<SettlementRecordVO> retrySettlement(@Valid @RequestBody RetrySettlementRequest request) {
        log.info("重试结算，settleNo={}", request.getSettleNo());
        SettlementRecordVO result = settlementService.retrySettlement(request.getSettleNo());
        return ApiResponse.success(result);
    }

    @PostMapping("/api/admin/settlements/reverse")
    @Operation(summary = "逆分账", description = "对成功结算的订单发起逆分账，将已分账资金按原路退回")
    @Idempotent
    public ApiResponse<ReverseSettlementVO> reverseSettlement(@Valid @RequestBody ReverseSettlementRequest request) {
        log.info("发起逆分账，settleNo={}, reason={}", request.getSettleNo(), request.getReason());
        ReverseSettlementVO result = settlementService.reverseSettlement(request.getSettleNo(), request.getReason());
        return ApiResponse.success(result);
    }
}
