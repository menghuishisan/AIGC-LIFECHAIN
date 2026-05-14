package com.lifechain.settlement.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.settlement.dto.BindSettleRuleRequest;
import com.lifechain.settlement.dto.WorkSettleRuleVO;
import com.lifechain.settlement.service.SettleRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 结算规则管理控制器
 * <p>
 * 提供作品与分账规则的绑定接口和生效规则查询接口。
 * 绑定操作仅管理员可执行，查询接口支持按作品编号获取当前生效规则。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "结算规则管理", description = "作品分账规则的绑定与查询")
public class SettleRuleController {

    private final SettleRuleService settleRuleService;

    /**
     * 绑定作品结算规则
     *
     * @param request 绑定请求
     * @return 规则视图对象
     */
    @PostMapping("/api/works/{workNo}/settle-rule")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "绑定结算规则", description = "将分账规则绑定到指定作品，平台+创作者比例之和须等于1")
    @Idempotent
    public ApiResponse<WorkSettleRuleVO> bindRule(@PathVariable String workNo,
                                                   @Valid @RequestBody BindSettleRuleRequest request) {
        request.setWorkNo(workNo);
        log.info("绑定结算规则，workNo={}", workNo);
        WorkSettleRuleVO result = settleRuleService.bindRule(request);
        return ApiResponse.success(result);
    }

    /**
     * 查询作品当前生效的结算规则
     *
     * @param workNo 作品编号
     * @return 规则视图对象
     */
    @GetMapping("/api/works/{workNo}/settle-rule")
    @Operation(summary = "查询生效规则", description = "查询指定作品当前生效的分账规则")
    public ApiResponse<WorkSettleRuleVO> getEffectiveRule(@PathVariable String workNo) {
        WorkSettleRuleVO result = settleRuleService.getEffectiveRule(workNo);
        return ApiResponse.success(result);
    }
}
