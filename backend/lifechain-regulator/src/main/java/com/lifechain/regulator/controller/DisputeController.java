package com.lifechain.regulator.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.CreateDisputeRequest;
import com.lifechain.regulator.dto.DisputeCaseVO;
import com.lifechain.regulator.dto.DisputeProcessRequest;
import com.lifechain.regulator.dto.AddEvidenceRequest;
import com.lifechain.regulator.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


/**
 * 争议案件控制器
 * <p>
 * 提供争议案件的完整生命周期管理接口：
 * <ul>
 *   <li>用户端：创建争议、补充证据、查询详情、查询我的争议列表</li>
 *   <li>管理端：处理争议（受理/审查/解决/驳回/关闭）</li>
 * </ul>
 * 关键节点的结论同步上链存证，确保争议处理的公信力和可追溯性。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "争议案件", description = "争议创建、证据补充、处理流转与查询")
public class DisputeController {

    private final DisputeService disputeService;

    /**
     * 创建争议案件（用户端）
     *
     * @param request 创建争议请求
     * @return 争议案件详情
     */
    @PostMapping("/api/disputes")
    @Operation(summary = "创建争议", description = "用户发起争议案件")
    @Idempotent
    public ApiResponse<DisputeCaseVO> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        Long accountId = UserContext.getUserId();
        log.info("创建争议案件，accountId={}, disputeType={}", accountId, request.getDisputeType());
        DisputeCaseVO result = disputeService.createDispute(accountId, request);
        return ApiResponse.success(result);
    }

    /**
     * 补充证据（用户端）
     *
     * @param caseNo 案件编号
     * @param body   请求体，包含evidenceType、fileUrl、description
     * @return 更新后的争议案件详情
     */
    @PostMapping("/api/disputes/{caseNo}/evidences")
    @Operation(summary = "补充证据", description = "向争议案件补充证据材料")
    @Idempotent
    public ApiResponse<DisputeCaseVO> addEvidence(
            @PathVariable String caseNo,
            @Valid @RequestBody AddEvidenceRequest request) {
        Long accountId = UserContext.getUserId();
        log.info("补充证据，accountId={}, caseNo={}, evidenceType={}", accountId, caseNo, request.getEvidenceType());
        DisputeCaseVO result = disputeService.addEvidence(accountId, caseNo,
                request.getEvidenceType(), request.getFileUrl(), request.getDescription(), request.getFileHash());
        return ApiResponse.success(result);
    }

    /**
     * 处理争议案件（管理端）
     *
     * @param caseNo  案件编号
     * @param request 处理请求
     * @return 更新后的争议案件详情
     */
    @PostMapping("/api/admin/disputes/process")
    @Operation(summary = "处理争议", description = "管理员处理争议案件（受理/审查/解决/驳回/关闭）")
    @Idempotent
    public ApiResponse<DisputeCaseVO> processDispute(
            @Valid @RequestBody DisputeProcessRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("处理争议案件，operatorId={}, caseNo={}, action={}", operatorId, request.getCaseNo(), request.getAction());
        DisputeCaseVO result = disputeService.processDispute(operatorId, request);
        return ApiResponse.success(result);
    }

    /**
     * 查询争议案件详情
     *
     * @param caseNo 案件编号
     * @return 争议案件详情（含证据和处理记录）
     */
    @GetMapping("/api/disputes/{caseNo}")
    @Operation(summary = "查询争议详情", description = "查询完整争议案件信息，包含证据和处理记录")
    public ApiResponse<DisputeCaseVO> getDetail(@PathVariable String caseNo) {
        DisputeCaseVO result = disputeService.getDisputeDetail(caseNo, UserContext.getUserId());
        return ApiResponse.success(result);
    }

    /**
     * 查询我的争议列表（用户端）
     *
     * @param status 争议状态（可选）
     * @param query  分页参数
     * @return 分页争议案件列表
     */
    @GetMapping("/api/disputes")
    @Operation(summary = "查询我的争议列表", description = "分页查询与当前用户相关的争议案件")
    public ApiResponse<PageResult<DisputeCaseVO>> listMyDisputes(
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        Long accountId = UserContext.getUserId();
        PageResult<DisputeCaseVO> result = disputeService.listDisputes(accountId, status, query);
        return ApiResponse.success(result);
    }
}
