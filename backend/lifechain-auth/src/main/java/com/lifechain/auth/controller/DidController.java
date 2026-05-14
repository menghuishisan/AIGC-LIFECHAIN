package com.lifechain.auth.controller;

import com.lifechain.auth.dto.DidApplyRequest;
import com.lifechain.auth.dto.DidInfoVO;
import com.lifechain.auth.service.DidService;
import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * DID（数字身份）控制器
 * <p>
 * 处理当前登录用户的DID申请和DID信息查询请求。
 * 用户通过实名认证后才可申请DID。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequestMapping("/api/did")
@RequiredArgsConstructor
@Tag(name = "DID管理", description = "数字身份申请与查询")
public class DidController {

    private final DidService didService;

    /**
     * 申请DID
     *
     * @param request 申请请求
     * @return 成功响应
     */
    @PostMapping("/apply")
    @Idempotent(key = "#request.requestId")
    @Operation(summary = "申请DID", description = "已通过实名认证的用户申请数字身份，系统自动生成DID编号和DID值")
    public ApiResponse<Void> applyDid(@Valid @RequestBody DidApplyRequest request) {
        Long accountId = UserContext.getUserId();
        log.info("申请DID，accountId={}", accountId);
        didService.applyDid(accountId, request);
        return ApiResponse.success();
    }

    /**
     * 查询DID信息
     *
     * @param didNo DID编号
     * @return DID信息
     */
    @GetMapping("/{didNo}")
    @Operation(summary = "查询DID信息", description = "根据DID编号查询数字身份详情，仅允许查看自己的DID")
    public ApiResponse<DidInfoVO> getDidInfo(@PathVariable String didNo) {
        Long accountId = UserContext.getUserId();
        log.info("查询DID信息，didNo={}, accountId={}", didNo, accountId);
        DidInfoVO didInfo = didService.getDidInfo(didNo, accountId);
        return ApiResponse.success(didInfo);
    }
}
