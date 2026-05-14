package com.lifechain.regulator.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.CreateRiskEventRequest;
import com.lifechain.regulator.dto.HandleRiskEventRequest;
import com.lifechain.regulator.dto.RiskEventVO;
import com.lifechain.regulator.service.RiskEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 风险事件管理控制器
 * <p>
 * 提供风险事件的创建、处理和查询接口，仅管理员可操作。
 * 风险事件由系统检测或管理员手动创建，处理后可触发冻结等后续操作。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "风险事件管理", description = "风险事件创建、处理与查询")
public class RiskEventController {

    private final RiskEventService riskEventService;

    /**
     * 创建风险事件
     *
     * @param request 创建请求
     * @return 风险事件详情
     */
    @PostMapping("/api/regulator/risk/mark")
    @Operation(summary = "风险标记", description = "监管方标记风险事件")
    @Idempotent
    public ApiResponse<RiskEventVO> createRiskEvent(@Valid @RequestBody CreateRiskEventRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("创建风险事件，operatorId={}, targetType={}", operatorId, request.getTargetType());
        RiskEventVO result = riskEventService.createRiskEvent(operatorId, request);
        return ApiResponse.success(result);
    }

    /**
     * 处理风险事件
     *
     * @param request 处理请求（包含riskNo）
     * @return 更新后的风险事件详情
     */
    @PostMapping("/api/regulator/risk/handle")
    @Operation(summary = "处理风险事件", description = "对风险事件执行确认、释放、冻结或审查操作")
    @Idempotent
    public ApiResponse<RiskEventVO> handleRiskEvent(@Valid @RequestBody HandleRiskEventRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("处理风险事件，operatorId={}, riskNo={}, action={}", operatorId, request.getRiskNo(), request.getAction());
        RiskEventVO result = riskEventService.handleRiskEvent(operatorId, request);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询待处理风险事件
     *
     * @param query 分页参数
     * @return 分页风险事件列表
     */
    @GetMapping("/api/regulator/risk")
    @Operation(summary = "查询待处理风险事件", description = "分页查询状态为已标记或审查中的风险事件")
    public ApiResponse<PageResult<RiskEventVO>> listPendingEvents(@Valid PageQuery query) {
        PageResult<RiskEventVO> result = riskEventService.listPendingEvents(query);
        return ApiResponse.success(result);
    }

    /**
     * 查询风险事件详情
     *
     * @param riskNo 风险编号
     * @return 风险事件详情
     */
    @GetMapping("/api/regulator/risk/{riskNo}")
    @Operation(summary = "查询风险事件详情", description = "根据风险编号查询完整风险事件信息")
    public ApiResponse<RiskEventVO> getDetail(@PathVariable String riskNo) {
        RiskEventVO result = riskEventService.getByRiskNo(riskNo);
        return ApiResponse.success(result);
    }
}
