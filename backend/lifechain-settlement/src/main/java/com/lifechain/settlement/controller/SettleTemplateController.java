package com.lifechain.settlement.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.settlement.dto.CreateSettleTemplateRequest;
import com.lifechain.settlement.dto.SettleTemplateVO;
import com.lifechain.settlement.service.SettleTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 结算模板管理控制器
 * <p>
 * 提供分账结算模板的创建、查询和列表接口，仅管理员可操作。
 * 模板定义了各角色在分账中的比例分配方案，绑定到作品后用于自动结算。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/settle/templates")
@RequiredArgsConstructor
@Tag(name = "结算模板管理", description = "分账模板的创建、查询与列表")
public class SettleTemplateController {

    private final SettleTemplateService settleTemplateService;

    /**
     * 创建结算模板
     *
     * @param request 创建请求
     * @return 模板视图对象
     */
    @PostMapping
    @Operation(summary = "创建结算模板", description = "创建新的分账模板及其明细，各角色比例之和必须等于1")
    @Idempotent
    public ApiResponse<SettleTemplateVO> createTemplate(@Valid @RequestBody CreateSettleTemplateRequest request) {
        log.info("创建结算模板，templateName={}", request.getTemplateName());
        SettleTemplateVO result = settleTemplateService.createTemplate(request);
        return ApiResponse.success(result);
    }

    /**
     * 查询结算模板详情
     *
     * @param templateCode 模板编码
     * @return 模板视图对象
     */
    @GetMapping("/{templateCode}")
    @Operation(summary = "查询模板详情", description = "根据模板编码查询包含明细的完整模板信息")
    public ApiResponse<SettleTemplateVO> getTemplate(@PathVariable String templateCode) {
        SettleTemplateVO result = settleTemplateService.getTemplate(templateCode);
        return ApiResponse.success(result);
    }

    /**
     * 查询所有生效中的模板列表
     *
     * @return 模板列表
     */
    @GetMapping
    @Operation(summary = "查询模板列表", description = "查询所有状态为ACTIVE的结算模板")
    public ApiResponse<List<SettleTemplateVO>> listTemplates() {
        List<SettleTemplateVO> result = settleTemplateService.listTemplates();
        return ApiResponse.success(result);
    }
}
