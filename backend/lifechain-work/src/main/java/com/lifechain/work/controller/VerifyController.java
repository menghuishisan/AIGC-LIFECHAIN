package com.lifechain.work.controller;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.infra.redis.RedisService;
import com.lifechain.work.dto.VerifyQueryLogVO;
import com.lifechain.work.dto.VerifyRequest;
import com.lifechain.work.dto.VerifyResultVO;
import com.lifechain.work.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 验真控制器。
 * <p>
 * 提供公开验真、登录用户验真、监管验真与验真日志查询能力。
 * 公开验真默认按真实客户端地址限流，只有在可信代理场景下才读取转发头。
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "验真查询", description = "作品版权验真查询服务")
public class VerifyController {

    private final CertificateService certificateService;
    private final AuditService auditService;
    private final RedisService redisService;

    private static final int PUBLIC_VERIFY_RATE_LIMIT = 30;
    private static final int REGULATOR_BATCH_VERIFY_LIMIT = 100;
    private static final long PUBLIC_VERIFY_WINDOW_SECONDS = 60;

    @Value("${app.security.trust-forward-headers}")
    private boolean trustForwardHeaders;

    /**
     * 公开验真查询。
     *
     * @param request 验真请求
     * @param servletRequest HTTP 请求
     * @return 验真结果摘要
     */
    @GetMapping("/public/verify")
    @Operation(summary = "公开验真", description = "无需登录的公开版权验真查询，只返回有限摘要信息")
    public ApiResponse<VerifyResultVO> verifyPublic(@Valid VerifyRequest request,
                                                    HttpServletRequest servletRequest) {
        String queryIp = getClientIp(servletRequest);
        checkPublicVerifyRateLimit(queryIp);
        VerifyResultVO result = certificateService.verifyPublic(request, queryIp);
        return ApiResponse.success(result);
    }

    /**
     * 登录用户验真查询。
     *
     * @param request 验真请求
     * @param servletRequest HTTP 请求
     * @return 详细验真结果
     */
    @GetMapping("/api/verify/detail")
    @Operation(summary = "登录用户验真", description = "登录用户版权验真查询，返回更详细的业务信息")
    public ApiResponse<VerifyResultVO> verifyLogin(@Valid VerifyRequest request,
                                                   HttpServletRequest servletRequest) {
        Long accountId = UserContext.getUserId();
        String queryIp = getClientIp(servletRequest);
        VerifyResultVO result = certificateService.verifyLogin(request, accountId, queryIp);
        return ApiResponse.success(result);
    }

    /**
     * 监管验真查询。
     *
     * @param request 验真请求
     * @param servletRequest HTTP 请求
     * @return 包含完整链上信息的验真结果
     */
    @GetMapping("/api/regulator/verify/detail")
    @Operation(summary = "监管方验真", description = "监管方版权验真查询，返回完整链上信息")
    public ApiResponse<VerifyResultVO> verifyRegulator(@Valid VerifyRequest request,
                                                       HttpServletRequest servletRequest) {
        Long accountId = UserContext.getUserId();
        String queryIp = getClientIp(servletRequest);
        VerifyResultVO result = certificateService.verifyRegulator(request, accountId, queryIp);
        auditService.writeAuditLog("VERIFY", null, request.getQueryValue(),
                "REGULATOR_VERIFY", "queryType=" + request.getQueryType(),
                accountId, "REGULATOR", queryIp, "SUCCESS", null);
        return ApiResponse.success(result);
    }

    /**
     * 查询验真日志。
     *
     * @param query 分页参数
     * @return 分页日志列表
     */
    @GetMapping("/api/admin/verify/logs")
    @Operation(summary = "验真查询日志", description = "管理员查看验真查询日志")
    public ApiResponse<PageResult<VerifyQueryLogVO>> listVerifyLogs(@Valid PageQuery query) {
        return ApiResponse.success(certificateService.listVerifyLogs(query));
    }

    /**
     * 监管批量验真。
     *
     * @param requests 批量验真请求列表
     * @param requestId 幂等请求 ID
     * @param servletRequest HTTP 请求
     * @return 批量验真结果
     */
    @PostMapping("/api/regulator/verify/batch")
    @Operation(summary = "批量验真", description = "监管方批量版权验真查询")
    @com.lifechain.common.annotation.Idempotent
    public ApiResponse<List<VerifyResultVO>> verifyBatch(
            @RequestBody List<VerifyRequest> requests,
            @RequestParam @jakarta.validation.constraints.NotBlank String requestId,
            HttpServletRequest servletRequest) {
        if (requests == null || requests.isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "批量验真请求不能为空");
        }
        if (requests.size() > REGULATOR_BATCH_VERIFY_LIMIT) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "单次批量验真数量不能超过 " + REGULATOR_BATCH_VERIFY_LIMIT + " 条");
        }

        Long accountId = UserContext.getUserId();
        String queryIp = getClientIp(servletRequest);
        List<VerifyResultVO> results = new ArrayList<>();
        for (VerifyRequest req : requests) {
            results.add(certificateService.verifyRegulator(req, accountId, queryIp));
        }
        auditService.writeAuditLog("VERIFY", null, null,
                "REGULATOR_BATCH_VERIFY", "count=" + requests.size(),
                accountId, "REGULATOR", queryIp, "SUCCESS", null);
        return ApiResponse.success(results);
    }

    /**
     * 校验公开验真的限流窗口。
     */
    private void checkPublicVerifyRateLimit(String ip) {
        String key = "rate:verify:public:" + ip;
        Long count = redisService.increment(key);
        if (count == 1) {
            redisService.expire(key, PUBLIC_VERIFY_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (count > PUBLIC_VERIFY_RATE_LIMIT) {
            log.warn("公开验真频率超限，IP={}, count={}", ip, count);
            throw new BizException(ErrorCodeEnum.RATE_LIMIT_EXCEEDED, "验真查询过于频繁，请稍后再试");
        }
    }

    /**
     * 获取客户端地址。
     * <p>
     * 默认只信任容器解析出的远端地址。
     * 仅在明确开启可信代理模式时才读取转发头。
     * </p>
     */
    private String getClientIp(HttpServletRequest request) {
        if (!trustForwardHeaders) {
            return request.getRemoteAddr();
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            return index != -1 ? ip.substring(0, index).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
