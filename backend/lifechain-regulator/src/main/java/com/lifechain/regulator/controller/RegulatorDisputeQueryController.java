package com.lifechain.regulator.controller;

import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.RegulatorDisputeListVO;
import com.lifechain.regulator.dto.RegulatorDisputeQuery;
import com.lifechain.regulator.service.DisputeService;
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
 * 监管员争议查询控制器
 * <p>
 * 提供监管员全量争议案件列表查询能力，支持多维度筛选。
 * 不改变用户侧 /api/disputes 的"我的争议列表"语义。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/regulator/disputes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('REGULATOR','PLATFORM_ADMIN')")
@Tag(name = "争议管理（监管员）", description = "监管员全量争议案件列表查询")
public class RegulatorDisputeQueryController {

    private final DisputeService disputeService;

    /**
     * 分页查询全量争议案件列表
     *
     * @param regulatorQuery 监管员筛选条件
     * @param pageQuery      分页参数
     * @return 分页争议列表
     */
    @GetMapping
    @Operation(summary = "查询全量争议列表", description = "监管员按条件分页查询所有争议案件")
    public ApiResponse<PageResult<RegulatorDisputeListVO>> listRegulatorDisputes(
            @Valid RegulatorDisputeQuery regulatorQuery,
            @Valid PageQuery pageQuery) {
        log.info("监管员查询争议列表，caseNo={}, status={}", regulatorQuery.getCaseNo(), regulatorQuery.getStatus());
        PageResult<RegulatorDisputeListVO> result = disputeService.listRegulatorDisputes(regulatorQuery, pageQuery);
        return ApiResponse.success(result);
    }
}
