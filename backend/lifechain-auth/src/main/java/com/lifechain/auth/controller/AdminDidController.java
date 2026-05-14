package com.lifechain.auth.controller;

import com.lifechain.auth.dto.DidInfoVO;
import com.lifechain.auth.dto.DidReviewRequest;
import com.lifechain.auth.service.DidService;
import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员-DID管理控制器
 * <p>
 * 处理管理员对DID的审核、挂起、吊销等操作。
 * DID审核通过后将自动触发Fabric链上注册。
 * 所有接口需要 PLATFORM_ADMIN 角色权限。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/did")
@RequiredArgsConstructor
@Tag(name = "管理员-DID管理", description = "DID审核、挂起、吊销")
public class AdminDidController {

    private final DidService didService;

    /**
     * 分页查询DID列表
     *
     * @param status    DID状态（可选）
     * @param accountNo 账户编号（可选）
     * @param query     分页参数
     * @return 分页DID列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询DID列表", description = "管理员分页查询所有DID记录，支持按状态和账户筛选")
    public ApiResponse<PageResult<DidInfoVO>> listDids(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String accountNo,
            @Valid PageQuery query) {
        log.info("管理员查询DID列表，status={}, accountNo={}", status, accountNo);
        PageResult<DidInfoVO> result = didService.listDids(status, accountNo, query);
        return ApiResponse.success(result);
    }

    /**
     * 审核DID
     *
     * @param request 审核请求
     * @return 成功响应
     */
    @PostMapping("/review")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "审核DID", description = "管理员审核DID申请，批准后自动上链注册")
    public ApiResponse<Void> reviewDid(@Valid @RequestBody DidReviewRequest request) {
        Long reviewerId = UserContext.getUserId();
        log.info("管理员审核DID，reviewerId={}, didNo={}", reviewerId, request.getDidNo());
        didService.reviewDid(reviewerId, request);
        return ApiResponse.success();
    }

    /**
     * 挂起DID
     *
     * @param didNo  DID编号
     * @param reason 挂起原因
     * @return 成功响应
     */
    @PostMapping("/suspend")
    @Operation(summary = "挂起DID", description = "管理员挂起已激活的DID，同步上链记录")
    @Idempotent
    public ApiResponse<Void> suspendDid(@Valid @RequestBody com.lifechain.auth.dto.DidOperationRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("管理员挂起DID，operatorId={}, didNo={}", operatorId, request.getDidNo());
        didService.suspendDid(operatorId, request.getDidNo(), request.getReason());
        return ApiResponse.success();
    }

    /**
     * 吊销DID
     *
     * @param didNo  DID编号
     * @param reason 吊销原因
     * @return 成功响应
     */
    @PostMapping("/revoke")
    @Operation(summary = "吊销DID", description = "管理员吊销已激活或已挂起的DID，同步上链记录")
    @Idempotent
    public ApiResponse<Void> revokeDid(@Valid @RequestBody com.lifechain.auth.dto.DidOperationRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("管理员吊销DID，operatorId={}, didNo={}", operatorId, request.getDidNo());
        didService.revokeDid(operatorId, request.getDidNo(), request.getReason());
        return ApiResponse.success();
    }
}
