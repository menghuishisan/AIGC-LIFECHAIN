package com.lifechain.regulator.controller;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.FreezeRecordVO;
import com.lifechain.regulator.dto.FreezeRequest;
import com.lifechain.regulator.dto.ReviewFreezeRequest;
import com.lifechain.regulator.dto.UnfreezeRequest;
import com.lifechain.regulator.service.FreezeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 冻结管理控制器
 * <p>
 * 提供冻结/解冻操作和冻结记录查询接口，仅管理员可操作。
 * 支持审核冻结和监管直接冻结两种模式，冻结信息同步上链存证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "冻结管理", description = "冻结/解冻操作与记录查询")
public class FreezeController {

    private final FreezeService freezeService;

    @PostMapping("/api/regulator/freeze/apply")
    @Operation(summary = "冻结申请", description = "监管发起冻结申请，需平台复核后生效")
    @Idempotent
    public ApiResponse<FreezeRecordVO> freezeApply(@Valid @RequestBody FreezeRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("冻结申请，operatorId={}, targetType={}, targetNo={}",
                operatorId, request.getTargetType(), request.getTargetNo());
        FreezeRecordVO result = freezeService.freeze(operatorId, request);
        return ApiResponse.success(result);
    }

    @PostMapping("/api/regulator/account/freeze/apply")
    @Operation(summary = "监管账户冻结申请", description = "监管方发起账户冻结申请")
    @Idempotent
    public ApiResponse<FreezeRecordVO> accountFreezeApply(@Valid @RequestBody FreezeRequest request) {
        Long operatorId = UserContext.getUserId();
        request.setTargetType("ACCOUNT");
        log.info("监管账户冻结申请，operatorId={}, targetNo={}", operatorId, request.getTargetNo());
        FreezeRecordVO result = freezeService.freeze(operatorId, request);
        return ApiResponse.success(result);
    }

    @PostMapping("/api/regulator/freeze/direct")
    @Operation(summary = "紧急直接冻结", description = "监管紧急直接冻结，跳过复核立即生效，需高级监管权限")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','REGULATOR')")
    @Idempotent
    public ApiResponse<FreezeRecordVO> freezeDirect(@Valid @RequestBody FreezeRequest request) {
        Long operatorId = UserContext.getUserId();
        request.setFreezeMode("REGULATOR_DIRECT");
        log.info("紧急直接冻结，operatorId={}, targetType={}, targetNo={}",
                operatorId, request.getTargetType(), request.getTargetNo());
        FreezeRecordVO result = freezeService.freeze(operatorId, request);
        return ApiResponse.success(result);
    }

    @PostMapping("/api/regulator/unfreeze/apply")
    @Operation(summary = "解冻申请", description = "对已生效的冻结记录发起解冻申请")
    @Idempotent
    public ApiResponse<FreezeRecordVO> unfreezeApply(@Valid @RequestBody UnfreezeRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("解冻申请，operatorId={}, freezeNo={}", operatorId, request.getFreezeNo());
        FreezeRecordVO result = freezeService.unfreeze(operatorId, request);
        return ApiResponse.success(result);
    }

    @GetMapping("/api/regulator/freeze/{freezeNo}")
    @Operation(summary = "查询冻结记录详情", description = "根据冻结编号查询完整冻结记录")
    public ApiResponse<FreezeRecordVO> getDetail(@PathVariable String freezeNo) {
        FreezeRecordVO result = freezeService.getFreezeRecord(freezeNo);
        return ApiResponse.success(result);
    }

    @GetMapping("/api/regulator/freeze")
    @Operation(summary = "查询冻结记录列表", description = "分页查询冻结记录，支持按目标类型和状态筛选")
    public ApiResponse<PageResult<FreezeRecordVO>> list(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String status,
            @Valid PageQuery query) {
        PageResult<FreezeRecordVO> result = freezeService.listFreezeRecords(targetType, status, query);
        return ApiResponse.success(result);
    }

    @PostMapping("/api/regulator/freeze/review")
    @Operation(summary = "事后复核直接冻结", description = "对紧急直接冻结进行事后复核，通过或驳回")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Idempotent(key = "#request.requestId")
    public ApiResponse<FreezeRecordVO> reviewFreeze(@Valid @RequestBody ReviewFreezeRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("事后复核冻结，operatorId={}, freezeNo={}, approved={}", operatorId, request.getFreezeNo(), request.getApproved());
        FreezeRecordVO result = freezeService.reviewFreeze(operatorId, request.getFreezeNo(), request.getApproved(), request.getReviewNote());
        return ApiResponse.success(result);
    }
}
