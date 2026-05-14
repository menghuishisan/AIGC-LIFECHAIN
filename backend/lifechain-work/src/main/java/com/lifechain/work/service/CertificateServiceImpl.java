package com.lifechain.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.auth.entity.DidRecordEntity;
import com.lifechain.auth.mapper.DidRecordMapper;
import com.lifechain.chain.adapter.ClaimChainAdapter;
import com.lifechain.chain.model.ChainQueryResult;
import com.lifechain.common.enums.*;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.infra.storage.StorageService;
import com.lifechain.work.assembler.WorkVoAssembler;
import com.lifechain.work.dto.CertDetailVO;
import com.lifechain.work.dto.VerifyQueryLogVO;
import com.lifechain.work.dto.VerifyRequest;
import com.lifechain.work.dto.VerifyResultVO;
import com.lifechain.work.entity.*;
import com.lifechain.work.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 证书服务实现
 * <p>
 * 实现证书生成、详情查询、下载以及多级别验真查询等功能。
 * 证书生成时创建JSON格式证书内容并上传到对象存储，计算证书哈希用于验真。
 * 验真查询分PUBLIC/LOGIN/REGULATOR三个级别，每次查询记录到查询日志。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateMapper certificateMapper;
    private final ClaimApplicationMapper claimApplicationMapper;
    private final WorkMapper workMapper;
    private final DidRecordMapper didRecordMapper;
    private final VerifyQueryLogMapper verifyQueryLogMapper;
    private final ClaimChainAdapter claimChainAdapter;
    private final StorageService storageService;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final CertificatePdfGenerator pdfGenerator;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertDetailVO generateCertificate(Long operatorId, String claimNo) {
        log.info("生成证书，operatorId={}, claimNo={}", operatorId, claimNo);

        // 校验确权成功
        ClaimApplicationEntity claim = claimApplicationMapper.selectByClaimNo(claimNo);
        if (claim == null) {
            throw new BizException(ErrorCodeEnum.CLAIM_NOT_FOUND);
        }
        if (!ClaimStatusEnum.CLAIM_SUCCESS.getCode().equals(claim.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "确权尚未成功，无法生成证书", null, claim.getStatus());
        }

        // 校验作品状态
        WorkEntity work = workMapper.selectById(claim.getWorkId());
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }
        if (!WorkStatusEnum.OWNERSHIP_CONFIRMED.getCode().equals(work.getStatus())) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_CONFIRMED,
                    "作品尚未确权成功", null, work.getStatus());
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        // 检查是否已存在有效证书（版本递增）
        int newVersion = 1;
        Long previousCertId = null;
        List<CertificateEntity> existingCerts = certificateMapper.selectByWorkId(work.getId());
        if (existingCerts != null && !existingCerts.isEmpty()) {
            CertificateEntity latestCert = existingCerts.get(0);
            if (CertStatusEnum.CERT_ACTIVE.getCode().equals(latestCert.getStatus())) {
                // 将旧证书设为失效
                String oldCertStatus = latestCert.getStatus();
                latestCert.setStatus(CertStatusEnum.CERT_INVALID.getCode());
                certificateMapper.updateById(latestCert);

                auditService.writeStatusHistory(
                        BizTypeEnum.CERTIFICATE.getCode(), latestCert.getId(), latestCert.getCertNo(),
                        oldCertStatus, CertStatusEnum.CERT_INVALID.getCode(),
                        "新版本证书生成，旧证书失效", null, operatorId);
            }
            newVersion = latestCert.getVersion() + 1;
            previousCertId = latestCert.getId();
        }

        // 创建证书记录（CERT_PENDING）
        CertificateEntity cert = new CertificateEntity();
        cert.setCertNo(BizNoUtil.certNo());
        cert.setWorkId(work.getId());
        cert.setWorkNo(work.getWorkNo());
        cert.setClaimId(claim.getId());
        cert.setClaimNo(claim.getClaimNo());
        cert.setHolderAccountId(claim.getApplicantAccountId());
        cert.setHolderDidId(claim.getApplicantDidId());
        cert.setStatus(CertStatusEnum.CERT_PENDING.getCode());
        cert.setVersion(newVersion);
        cert.setPreviousCertId(previousCertId);
        certificateMapper.insert(cert);

        auditService.writeStatusHistory(
                BizTypeEnum.CERTIFICATE.getCode(), cert.getId(), cert.getCertNo(),
                "", CertStatusEnum.CERT_PENDING.getCode(),
                "证书创建", null, operatorId);

        // 设置为生成中
        cert.setStatus(CertStatusEnum.CERT_GENERATING.getCode());
        certificateMapper.updateById(cert);

        auditService.writeStatusHistory(
                BizTypeEnum.CERTIFICATE.getCode(), cert.getId(), cert.getCertNo(),
                CertStatusEnum.CERT_PENDING.getCode(), CertStatusEnum.CERT_GENERATING.getCode(),
                "开始生成证书内容", null, operatorId);

        // 获取创作者DID信息
        DidRecordEntity didRecord = didRecordMapper.selectById(claim.getApplicantDidId());
        String creatorDid = didRecord != null ? didRecord.getDidValue() : "";

        // 生成证书内容（JSON格式）
        String certContent = buildCertContent(cert, claim, work, creatorDid, now);
        String certHash = HashUtil.sha256(certContent);

        // 上传JSON证书内容到对象存储
        String jsonObjectName = "certificates/" + cert.getCertNo() + "/cert.json";
        storageService.uploadFile(jsonObjectName,
                certContent.getBytes(StandardCharsets.UTF_8), "application/json");

        // 生成并上传PDF证书文件
        byte[] pdfBytes = pdfGenerator.generatePdf(cert, claim, work, creatorDid, now);
        String pdfObjectName = "certificates/" + cert.getCertNo() + "/cert.pdf";
        String certFileUrl = storageService.uploadFile(pdfObjectName, pdfBytes, "application/pdf");

        // 更新证书为有效状态
        cert.setStatus(CertStatusEnum.CERT_ACTIVE.getCode());
        cert.setCertHash(certHash);
        cert.setCertFileUrl(certFileUrl);
        cert.setIssueTime(now);
        cert.setExpireTime(now.plusYears(10));
        certificateMapper.updateById(cert);

        auditService.writeStatusHistory(
                BizTypeEnum.CERTIFICATE.getCode(), cert.getId(), cert.getCertNo(),
                CertStatusEnum.CERT_GENERATING.getCode(), CertStatusEnum.CERT_ACTIVE.getCode(),
                "证书生成完成", null, operatorId);

        auditService.writeAuditLog(
                BizTypeEnum.CERTIFICATE.getCode(), cert.getId(), cert.getCertNo(),
                "GENERATE", "证书生成成功，certHash=" + certHash,
                operatorId, null, null, "SUCCESS", null);

        traceEventService.writeTraceEvent(BizTypeEnum.CERTIFICATE.getCode(), cert.getId(), cert.getCertNo(),
                "CERTIFICATE_GENERATED", "证书生成成功", operatorId, null, null);

        log.info("证书生成成功，certNo={}, version={}", cert.getCertNo(), cert.getVersion());

        return toCertDetailVO(cert);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CertDetailVO getCertificateDetail(String certNo, Long viewerAccountId) {
        CertificateEntity cert = certificateMapper.selectByCertNo(certNo);
        if (cert == null) {
            throw new BizException(ErrorCodeEnum.CERT_NOT_FOUND);
        }
        // 归属校验：仅证书持有人或管理员/监管员可查看
        if (!cert.getHolderAccountId().equals(viewerAccountId) && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该证书详情");
        }
        return toCertDetailVO(cert);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] downloadCertificate(String certNo, Long viewerAccountId) {
        CertificateEntity cert = certificateMapper.selectByCertNo(certNo);
        if (cert == null) {
            throw new BizException(ErrorCodeEnum.CERT_NOT_FOUND);
        }
        // 归属校验：仅证书持有人或管理员/监管员可下载
        if (!cert.getHolderAccountId().equals(viewerAccountId) && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权下载该证书");
        }

        String objectName = "certificates/" + cert.getCertNo() + "/cert.pdf";
        try (InputStream is = storageService.downloadFile(objectName)) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new BizException(ErrorCodeEnum.STORAGE_DOWNLOAD_FAILED, "证书文件下载失败", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResultVO verifyPublic(VerifyRequest request, String queryIp) {
        return doVerify(request, null, queryIp, "PUBLIC");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResultVO verifyLogin(VerifyRequest request, Long accountId, String queryIp) {
        return doVerify(request, accountId, queryIp, "LOGIN");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyResultVO verifyRegulator(VerifyRequest request, Long accountId, String queryIp) {
        return doVerify(request, accountId, queryIp, "REGULATOR");
    }

    // ==================== 私有方法 ====================

    /**
     * 统一验真查询逻辑
     * <p>
     * 根据查询类型（CERT_NO/WORK_NO/FILE_HASH）查找匹配的证书和确权信息，
     * 根据验证级别返回不同详细程度的结果，并记录查询日志。
     * </p>
     *
     * @param request   验真请求
     * @param accountId 查询人账户ID（PUBLIC级别可为null）
     * @param queryIp   查询方IP
     * @param source    查询来源级别
     * @return 验真结果
     */
    private VerifyResultVO doVerify(VerifyRequest request, Long accountId,
                                    String queryIp, String source) {
        log.info("验真查询，type={}, value={}, source={}", request.getQueryType(), request.getQueryValue(), source);

        CertificateEntity cert = null;
        ClaimApplicationEntity claim = null;
        WorkEntity work = null;

        String queryType = request.getQueryType();
        String queryValue = request.getQueryValue();

        switch (queryType) {
            case "CERT_NO":
                cert = certificateMapper.selectByCertNo(queryValue);
                if (cert != null) {
                    claim = claimApplicationMapper.selectById(cert.getClaimId());
                    work = workMapper.selectById(cert.getWorkId());
                }
                break;
            case "WORK_NO":
                work = workMapper.selectByWorkNo(queryValue);
                if (work != null) {
                    List<CertificateEntity> certs = certificateMapper.selectByWorkId(work.getId());
                    if (certs != null && !certs.isEmpty()) {
                        cert = certs.get(0);
                        claim = claimApplicationMapper.selectById(cert.getClaimId());
                    }
                }
                break;
            case "FILE_HASH":
                LambdaQueryWrapper<WorkEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(WorkEntity::getFileHash, queryValue);
                List<WorkEntity> works = workMapper.selectList(wrapper);
                if (works != null && !works.isEmpty()) {
                    work = works.get(0);
                    List<CertificateEntity> certs = certificateMapper.selectByWorkId(work.getId());
                    if (certs != null && !certs.isEmpty()) {
                        cert = certs.get(0);
                        claim = claimApplicationMapper.selectById(cert.getClaimId());
                    }
                }
                break;
            default:
                throw new BizException(ErrorCodeEnum.PARAM_INVALID, "不支持的查询类型：" + queryType);
        }

        boolean matchFound = cert != null;

        // 构建验真结果
        VerifyResultVO.VerifyResultVOBuilder builder = VerifyResultVO.builder()
                .verified(matchFound)
                .verifyLevel(source);

        if (cert != null) {
            builder.certNo(cert.getCertNo())
                    .certStatus(cert.getStatus());

            if (work != null) {
                builder.workNo(work.getWorkNo());
            }
            if (claim != null) {
                builder.claimTime(DateTimeUtil.formatUtc(claim.getApproveTime()));
                builder.summaryHash(claim.getSummaryHash());
            }

            // LOGIN和REGULATOR返回更多信息
            if ("LOGIN".equals(source) || "REGULATOR".equals(source)) {
                if (claim != null) {
                    DidRecordEntity didRecord = didRecordMapper.selectById(claim.getApplicantDidId());
                    if (didRecord != null) {
                        builder.creatorDid(didRecord.getDidValue());
                    }
                }
            }

            // REGULATOR返回完整信息
            if ("REGULATOR".equals(source)) {
                if (claim != null) {
                    builder.chainTxHash(claim.getTxHash())
                            .blockHeight(claim.getBlockHeight() != null
                                    ? claim.getBlockHeight().toString() : null);
                }

                // 交叉验证链上记录
                if (claim != null && claim.getTxHash() != null) {
                    ChainQueryResult chainResult = claimChainAdapter.queryClaim(claim.getClaimNo());
                    if (chainResult.isSuccess()) {
                        log.info("链上交叉验证通过，claimNo={}", claim.getClaimNo());
                    }
                }
            }
        }

        VerifyResultVO result = builder.build();

        // 记录查询日志
        saveVerifyQueryLog(queryType, queryValue, source, accountId, queryIp, matchFound,
                cert != null ? "证书编号=" + cert.getCertNo() : "未匹配到记录");

        return result;
    }

    /**
     * 构建证书内容（JSON格式）
     */
    private String buildCertContent(CertificateEntity cert, ClaimApplicationEntity claim,
                                    WorkEntity work, String creatorDid, LocalDateTime now) {
        return "{"
                + "\"certNo\":\"" + cert.getCertNo() + "\","
                + "\"workNo\":\"" + work.getWorkNo() + "\","
                + "\"workTitle\":\"" + escapeJson(work.getTitle()) + "\","
                + "\"workType\":\"" + work.getWorkType() + "\","
                + "\"claimNo\":\"" + claim.getClaimNo() + "\","
                + "\"creatorDid\":\"" + creatorDid + "\","
                + "\"fileHash\":\"" + (work.getFileHash() != null ? work.getFileHash() : "") + "\","
                + "\"metaHash\":\"" + (work.getMetaHash() != null ? work.getMetaHash() : "") + "\","
                + "\"summaryHash\":\"" + (claim.getSummaryHash() != null ? claim.getSummaryHash() : "") + "\","
                + "\"txHash\":\"" + (claim.getTxHash() != null ? claim.getTxHash() : "") + "\","
                + "\"blockHeight\":" + (claim.getBlockHeight() != null ? claim.getBlockHeight() : "null") + ","
                + "\"version\":" + cert.getVersion() + ","
                + "\"issueTime\":\"" + DateTimeUtil.formatUtc(now) + "\","
                + "\"expireTime\":\"" + DateTimeUtil.formatUtc(now.plusYears(10)) + "\""
                + "}";
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 保存验真查询日志
     */
    private void saveVerifyQueryLog(String queryType, String queryValue, String querySource,
                                    Long accountId, String queryIp, boolean matchFound,
                                    String resultSummary) {
        VerifyQueryLogEntity logEntity = new VerifyQueryLogEntity();
        logEntity.setQueryType(queryType);
        logEntity.setQueryValue(queryValue);
        logEntity.setQuerySource(querySource);
        logEntity.setQueryAccountId(accountId);
        logEntity.setQueryIp(queryIp);
        logEntity.setMatchFound(matchFound ? 1 : 0);
        logEntity.setResultSummary(resultSummary);
        logEntity.setQueryTime(DateTimeUtil.nowUtc());
        verifyQueryLogMapper.insert(logEntity);
    }

    /**
     * 转换为证书详情视图对象
     */
    private CertDetailVO toCertDetailVO(CertificateEntity entity) {
        CertDetailVO vo = new CertDetailVO();

        var basic = new CertDetailVO.BasicInfo();
        basic.setCertNo(entity.getCertNo());
        basic.setCertHash(entity.getCertHash());
        basic.setCertFileUrl(entity.getCertFileUrl());
        basic.setVersion(entity.getVersion());
        vo.setBasicInfo(basic);

        var status = new CertDetailVO.StatusInfo();
        status.setStatus(entity.getStatus());
        vo.setStatusInfo(status);

        var time = new CertDetailVO.TimeInfo();
        time.setIssueTime(entity.getIssueTime());
        time.setExpireTime(entity.getExpireTime());
        vo.setTimeInfo(time);

        var relation = new CertDetailVO.RelationInfo();
        relation.setWorkNo(entity.getWorkNo());
        relation.setClaimNo(entity.getClaimNo());
        vo.setRelationInfo(relation);

        vo.setChainInfo(new CertDetailVO.ChainInfo());
        vo.setAllowedActions(List.of());

        // 统一可见性装配
        WorkVoAssembler.applyVisibility(vo);

        return vo;
    }

    @Override
    public PageResult<VerifyQueryLogVO> listVerifyLogs(PageQuery query) {
        LambdaQueryWrapper<VerifyQueryLogEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(VerifyQueryLogEntity::getQueryTime);
        Page<VerifyQueryLogEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<VerifyQueryLogEntity> result = verifyQueryLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords().stream().map(VerifyQueryLogVO::fromEntity).toList(),
                result.getTotal(), query.getPageNo(), query.getPageSize());
    }
}
