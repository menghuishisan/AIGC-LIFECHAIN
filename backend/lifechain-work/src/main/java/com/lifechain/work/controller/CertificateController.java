package com.lifechain.work.controller;

import com.lifechain.common.context.UserContext;
import com.lifechain.common.model.ApiResponse;
import com.lifechain.work.dto.CertDetailVO;
import com.lifechain.work.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 证书控制器
 * <p>
 * 处理证书生成（管理员）、证书详情查询和证书下载等请求。
 * 生成接口需要管理员权限，查询和下载需要登录认证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "证书管理", description = "证书生成、查询与下载")
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * 生成证书（管理员）
     *
     * @param claimNo 确权编号
     * @return 证书详情
     */
    @PostMapping("/api/admin/certificates/generate")
    @Operation(summary = "生成证书", description = "管理员根据已成功的确权申请生成数字版权证书")
    @com.lifechain.common.annotation.Idempotent
    public ApiResponse<CertDetailVO> generateCertificate(@jakarta.validation.Valid @RequestBody com.lifechain.work.dto.GenerateCertificateRequest request) {
        Long operatorId = UserContext.getUserId();
        log.info("生成证书，operatorId={}, claimNo={}", operatorId, request.getClaimNo());
        CertDetailVO result = certificateService.generateCertificate(operatorId, request.getClaimNo());
        return ApiResponse.success(result);
    }

    /**
     * 查询证书详情
     *
     * @param certNo 证书编号
     * @return 证书详情
     */
    @GetMapping("/api/certificates/{certNo}")
    @Operation(summary = "证书详情", description = "查询证书的完整详情信息")
    public ApiResponse<CertDetailVO> getCertificateDetail(@PathVariable String certNo) {
        CertDetailVO result = certificateService.getCertificateDetail(certNo, UserContext.getUserId());
        return ApiResponse.success(result);
    }

    /**
     * 下载证书文件
     *
     * @param certNo 证书编号
     * @return 证书文件字节数组
     */
    @GetMapping("/api/certificates/{certNo}/download")
    @Operation(summary = "下载证书", description = "下载证书文件（PDF格式）")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String certNo) {
        byte[] data = certificateService.downloadCertificate(certNo, UserContext.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + certNo + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
