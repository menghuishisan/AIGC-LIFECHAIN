package com.lifechain.work.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.auth.entity.DidRecordEntity;
import com.lifechain.auth.mapper.DidRecordMapper;
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
import com.lifechain.work.dto.*;
import com.lifechain.work.entity.*;
import com.lifechain.work.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 作品服务实现
 * <p>
 * 实现作品上传、详情查询、元数据更新、特征提取、我的作品列表和市场作品列表等功能。
 * 所有状态变更均写入状态变更历史，关键操作写入审计日志。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {

    /** 允许上传的 MIME 类型白名单 */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/quicktime", "video/x-msvideo",
            "audio/mpeg", "audio/wav", "audio/ogg",
            "application/pdf",
            "model/gltf-binary", "model/gltf+json"
    );

    /** 单文件最大大小：500 MB */
    private static final long MAX_FILE_SIZE_BYTES = 500L * 1024 * 1024;

    private final WorkMapper workMapper;
    private final WorkFileMapper workFileMapper;
    private final WorkAigcMetaMapper workAigcMetaMapper;
    private final WorkFeatureMapper workFeatureMapper;
    private final WorkSimilarityCheckMapper workSimilarityCheckMapper;
    private final ClaimApplicationMapper claimApplicationMapper;
    private final CertificateMapper certificateMapper;
    private final DidRecordMapper didRecordMapper;
    private final StorageService storageService;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final FeatureExtractService featureExtractService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkDetailVO uploadWork(Long accountId, WorkUploadRequest request, List<MultipartFile> files) {
        log.info("上传作品，accountId={}, title={}", accountId, request.getTitle());

        // 校验DID已生效
        DidRecordEntity didRecord = didRecordMapper.selectByAccountId(accountId);
        if (didRecord == null || !DidStatusEnum.DID_ACTIVE.getCode().equals(didRecord.getStatus())) {
            throw new BizException(ErrorCodeEnum.DID_NOT_ACTIVE, "请先完成DID认证");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();

        // 创建作品记录
        WorkEntity work = new WorkEntity();
        work.setWorkNo(BizNoUtil.workNo());
        work.setCreatorAccountId(accountId);
        work.setCreatorSubjectId(didRecord.getSubjectId());
        work.setCreatorDidId(didRecord.getId());
        work.setTitle(request.getTitle());
        work.setDescription(request.getDescription());
        work.setWorkType(request.getWorkType());
        work.setStatus(WorkStatusEnum.UPLOADED.getCode());
        work.setCoverUrl(request.getCoverUrl());
        work.setSubmitTime(now);
        workMapper.insert(work);

        log.info("作品记录创建成功，workNo={}, workId={}", work.getWorkNo(), work.getId());

        // 上传文件并保存文件记录，同时拼接哈希源
        StringBuilder hashSource = new StringBuilder();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                WorkFileEntity fileEntity = uploadAndSaveFile(work.getId(), file, "ORIGINAL", now);
                if (fileEntity.getFileHash() != null) {
                    hashSource.append(fileEntity.getFileHash());
                }
            }
        }

        // 计算文件哈希（所有文件哈希拼接后再计算SHA-256）
        if (!hashSource.isEmpty()) {
            work.setFileHash(HashUtil.sha256(hashSource.toString()));
        }

        // 保存AIGC元数据
        if (request.getAigcMeta() != null) {
            saveAigcMeta(work.getId(), request.getAigcMeta());
        }

        // 计算元数据哈希
        String metaHashSource = work.getTitle() + "|" + work.getWorkType() + "|"
                + (work.getDescription() != null ? work.getDescription() : "");
        work.setMetaHash(HashUtil.sha256(metaHashSource));
        workMapper.updateById(work);

        // 写入状态变更历史
        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "", WorkStatusEnum.UPLOADED.getCode(),
                "作品上传", null, accountId);

        // 写入审计日志
        auditService.writeAuditLog(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "UPLOAD", "上传作品：" + work.getTitle(),
                accountId, null, null, "SUCCESS", null);

        traceEventService.writeTraceEvent(BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "WORK_UPLOADED", "作品上传完成", accountId, null, null);

        return getWorkDetail(work.getWorkNo(), accountId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkDetailVO getWorkDetail(String workNo, Long viewerAccountId) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }

        // 访问边界：非本人、非管理/监管角色只能查看已上架作品
        boolean isOwner = viewerAccountId != null
                && viewerAccountId.equals(work.getCreatorAccountId());
        boolean isPrivileged = FieldVisibilityUtil.isPrivilegedViewer();
        if (!isOwner && !isPrivileged
                && !WorkStatusEnum.LISTED.getCode().equals(work.getStatus())) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }

        WorkDetailVO vo = new WorkDetailVO();

        var basic = new WorkDetailVO.BasicInfo();
        basic.setWorkNo(work.getWorkNo());
        basic.setTitle(work.getTitle());
        basic.setDescription(work.getDescription());
        basic.setWorkType(work.getWorkType());
        basic.setCoverUrl(work.getCoverUrl());
        vo.setBasicInfo(basic);

        var statusInfo = new WorkDetailVO.StatusInfo();
        statusInfo.setStatus(work.getStatus());
        vo.setStatusInfo(statusInfo);

        var time = new WorkDetailVO.TimeInfo();
        time.setCreatedAt(work.getCreatedAt());
        time.setSubmitTime(work.getSubmitTime());
        time.setUpdatedAt(work.getUpdatedAt());
        vo.setTimeInfo(time);

        // 加载文件列表
        List<WorkFileEntity> fileEntities = workFileMapper.selectByWorkId(work.getId());
        basic.setFiles(fileEntities.stream().map(this::toWorkFileVO).collect(Collectors.toList()));

        // 加载AIGC元数据
        WorkAigcMetaEntity aigcMeta = workAigcMetaMapper.selectByWorkId(work.getId());
        if (aigcMeta != null) {
            basic.setAigcMeta(toAigcMetaDTO(aigcMeta));
        }

        // 加载特征信息
        WorkFeatureEntity feature = workFeatureMapper.selectByWorkId(work.getId());
        if (feature != null) {
            basic.setFeature(toWorkFeatureVO(feature));
        }

        var relation = new WorkDetailVO.RelationInfo();
        var chain = new WorkDetailVO.ChainInfo();

        // 加载确权信息
        List<ClaimApplicationEntity> claims = claimApplicationMapper.selectByWorkId(work.getId());
        if (claims != null && !claims.isEmpty()) {
            ClaimApplicationEntity latestClaim = claims.get(0);
            relation.setClaimNo(latestClaim.getClaimNo());
            chain.setTxHash(latestClaim.getTxHash());
            chain.setBlockHeight(latestClaim.getBlockHeight());
        }

        // 加载证书信息
        List<CertificateEntity> certs = certificateMapper.selectByWorkId(work.getId());
        if (certs != null && !certs.isEmpty()) {
            relation.setCertNo(certs.get(0).getCertNo());
        }

        vo.setRelationInfo(relation);
        vo.setChainInfo(chain);

        // 计算允许操作
        vo.setAllowedActions(computeAllowedActions(work, viewerAccountId));

        // 统一可见性装配
        WorkVoAssembler.applyVisibility(vo, isOwner);

        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkMeta(Long accountId, String workNo, WorkMetaUpdateRequest request) {
        log.info("更新作品元数据，accountId={}, workNo={}", accountId, workNo);

        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }
        if (!work.getCreatorAccountId().equals(accountId)) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权操作此作品");
        }

        String status = work.getStatus();
        if (!WorkStatusEnum.DRAFT.getCode().equals(status)
                && !WorkStatusEnum.UPLOADED.getCode().equals(status)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前状态不允许更新元数据", null, status);
        }

        // 更新基本信息
        if (request.getTitle() != null) {
            work.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            work.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            work.setCoverUrl(request.getCoverUrl());
        }

        // 更新AIGC元数据
        if (request.getAigcMeta() != null) {
            WorkAigcMetaEntity existingMeta = workAigcMetaMapper.selectByWorkId(work.getId());
            if (existingMeta != null) {
                updateAigcMetaFields(existingMeta, request.getAigcMeta());
                workAigcMetaMapper.updateById(existingMeta);
            } else {
                saveAigcMeta(work.getId(), request.getAigcMeta());
            }
        }

        // 重新计算元数据哈希
        String metaHashSource = work.getTitle() + "|" + work.getWorkType() + "|"
                + (work.getDescription() != null ? work.getDescription() : "");
        work.setMetaHash(HashUtil.sha256(metaHashSource));
        workMapper.updateById(work);

        // 写入审计日志
        auditService.writeAuditLog(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "UPDATE_META", "更新作品元数据",
                accountId, null, null, "SUCCESS", null);

        traceEventService.writeTraceEvent(BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "WORK_META_UPDATED", "作品元数据更新", accountId, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerFeatureExtract(Long accountId, String workNo) {
        log.info("触发特征提取，accountId={}, workNo={}", accountId, workNo);

        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }
        if (!work.getCreatorAccountId().equals(accountId)) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权操作此作品");
        }
        if (!WorkStatusEnum.UPLOADED.getCode().equals(work.getStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "仅已上传状态的作品可触发特征提取", null, work.getStatus());
        }

        LocalDateTime now = DateTimeUtil.nowUtc();
        String oldStatus = work.getStatus();

        // 设置为特征提取中
        work.setStatus(WorkStatusEnum.FEATURE_PENDING.getCode());
        workMapper.updateById(work);

        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                oldStatus, WorkStatusEnum.FEATURE_PENDING.getCode(),
                "触发特征提取", null, accountId);

        // 调用特征提取服务
        String fileHash = work.getFileHash() != null ? work.getFileHash() : work.getWorkNo();
        FeatureExtractService.FeatureResult extractResult =
                featureExtractService.extract(fileHash, work.getWorkType(), null);

        WorkFeatureEntity feature = new WorkFeatureEntity();
        feature.setWorkId(work.getId());
        feature.setFeatureType(extractResult.featureType());
        feature.setFeatureValue(extractResult.featureValue());
        feature.setPerceptualHash(extractResult.perceptualHash());
        feature.setExtractStatus("SUCCESS");
        feature.setExtractTime(now);
        workFeatureMapper.insert(feature);

        // 执行相似度检测：与已有作品特征进行比对
        boolean highRisk = runSimilarityCheck(work, extractResult.perceptualHash(), now);

        // 高风险作品进入人工复核态，不允许直接确权
        String newStatus;
        if (highRisk) {
            newStatus = WorkStatusEnum.SIMILARITY_HIGH_RISK.getCode();
            log.warn("作品相似度高风险，进入人工复核态，workNo={}", workNo);
        } else {
            newStatus = WorkStatusEnum.READY_FOR_CLAIM.getCode();
        }

        work.setStatus(newStatus);
        workMapper.updateById(work);

        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                WorkStatusEnum.FEATURE_PENDING.getCode(), newStatus,
                highRisk ? "特征提取完成，检测到高相似度" : "特征提取完成", null, accountId);

        auditService.writeAuditLog(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "FEATURE_EXTRACT", "特征提取完成，感知哈希=" + extractResult.perceptualHash(),
                accountId, null, null, "SUCCESS", null);

        traceEventService.writeTraceEvent(BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "WORK_FEATURE_EXTRACTED", "特征提取完成", accountId, null, null);

        log.info("特征提取完成，workNo={}, perceptualHash={}", workNo, extractResult.perceptualHash());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkFeatureVO getWorkFeature(String workNo, Long viewerAccountId) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND);
        }
        // 归属校验：仅作品创作者或管理员/监管员可查看特征信息
        if (!work.getCreatorAccountId().equals(viewerAccountId) && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该作品特征信息");
        }

        WorkFeatureEntity feature = workFeatureMapper.selectByWorkId(work.getId());
        if (feature == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "作品尚未进行特征提取");
        }

        return toWorkFeatureVO(feature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<WorkListVO> listMyWorks(Long accountId, String status, PageQuery query) {
        LambdaQueryWrapper<WorkEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkEntity::getCreatorAccountId, accountId)
                .eq(status != null && !status.isBlank(), WorkEntity::getStatus, status)
                .orderByDesc(WorkEntity::getCreatedAt);

        Page<WorkEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<WorkEntity> result = workMapper.selectPage(page, wrapper);

        List<WorkListVO> records = result.getRecords().stream()
                .map(this::toWorkListVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<WorkListVO> listMarketWorks(String workType, String keyword, PageQuery query) {
        LambdaQueryWrapper<WorkEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkEntity::getStatus, WorkStatusEnum.LISTED.getCode())
                .eq(workType != null && !workType.isBlank(), WorkEntity::getWorkType, workType)
                .like(keyword != null && !keyword.isBlank(), WorkEntity::getTitle, keyword)
                .orderByDesc(WorkEntity::getCreatedAt);

        Page<WorkEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        Page<WorkEntity> result = workMapper.selectPage(page, wrapper);

        List<WorkListVO> records = result.getRecords().stream()
                .map(this::toWorkListVO)
                .collect(Collectors.toList());

        return PageResult.of(records, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkDetailVO getMarketWorkDetail(String workNo) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null || !WorkStatusEnum.LISTED.getCode().equals(work.getStatus())) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_FOUND, "作品不存在或未上架");
        }
        return getWorkDetail(workNo, null);
    }

    // ==================== 私有方法 ====================

    /**
     * 上传文件到对象存储并保存文件记录
     *
     * @param workId  作品ID
     * @param file    上传文件
     * @param purpose 用途
     * @param now     当前时间
     * @return 文件实体
     */
    private WorkFileEntity uploadAndSaveFile(Long workId, MultipartFile file, String purpose, LocalDateTime now) {
        // 文件类型白名单校验（不信任客户端上传的 Content-Type，同时做基础检查）
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "不支持的文件类型: " + contentType + "，仅允许图片、视频、音频、PDF及3D模型");
        }

        // 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID,
                    "文件超过最大限制（500MB）：" + file.getOriginalFilename());
        }

        // 防路径遍历：使用 UUID 作为存储对象名，原始文件名仅做记录
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            String rawExt = originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
            // 只保留字母数字，防止注入
            ext = "." + rawExt.replaceAll("[^a-zA-Z0-9]", "");
        }
        String objectName = "works/" + workId + "/" + purpose.toLowerCase() + "/" + UUID.randomUUID() + ext;

        String fileUrl;
        String fileHash;
        try {
            byte[] fileData = file.getBytes();
            fileHash = HashUtil.sha256(fileData);
            fileUrl = storageService.uploadFile(objectName, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException e) {
            throw new BizException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED, "文件上传失败：" + originalFilename, e);
        }

        WorkFileEntity fileEntity = new WorkFileEntity();
        fileEntity.setWorkId(workId);
        fileEntity.setFileName(originalFilename);
        fileEntity.setFilePath(objectName);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(contentType);
        fileEntity.setFileHash(fileHash);
        fileEntity.setFileUrl(fileUrl);
        fileEntity.setPurpose(purpose);
        workFileMapper.insert(fileEntity);

        return fileEntity;
    }

    /**
     * 保存AIGC元数据
     *
     * @param workId  作品ID
     * @param metaDTO AIGC元数据DTO
     */
    private void saveAigcMeta(Long workId, AigcMetaDTO metaDTO) {
        WorkAigcMetaEntity meta = new WorkAigcMetaEntity();
        meta.setWorkId(workId);
        meta.setAigcTool(metaDTO.getAigcTool());
        meta.setAigcModel(metaDTO.getAigcModel());
        meta.setAigcVersion(metaDTO.getAigcVersion());
        meta.setPromptSummary(metaDTO.getPromptSummary());
        meta.setGenerationParams(metaDTO.getGenerationParams());
        if (metaDTO.getGenerationTime() != null) {
            meta.setGenerationTime(DateTimeUtil.parseUtc(metaDTO.getGenerationTime()));
        }
        workAigcMetaMapper.insert(meta);
    }

    /**
     * 更新AIGC元数据字段
     *
     * @param entity  现有实体
     * @param metaDTO 新的元数据DTO
     */
    private void updateAigcMetaFields(WorkAigcMetaEntity entity, AigcMetaDTO metaDTO) {
        if (metaDTO.getAigcTool() != null) {
            entity.setAigcTool(metaDTO.getAigcTool());
        }
        if (metaDTO.getAigcModel() != null) {
            entity.setAigcModel(metaDTO.getAigcModel());
        }
        if (metaDTO.getAigcVersion() != null) {
            entity.setAigcVersion(metaDTO.getAigcVersion());
        }
        if (metaDTO.getPromptSummary() != null) {
            entity.setPromptSummary(metaDTO.getPromptSummary());
        }
        if (metaDTO.getGenerationParams() != null) {
            entity.setGenerationParams(metaDTO.getGenerationParams());
        }
        if (metaDTO.getGenerationTime() != null) {
            entity.setGenerationTime(DateTimeUtil.parseUtc(metaDTO.getGenerationTime()));
        }
    }

    /**
     * 执行相似度检测
     * <p>
     * 将当前作品的感知哈希与所有已提取特征的作品进行比对，
     * 计算汉明距离确定性相似度分数，保存检测结果。
     * </p>
     *
     * @param work           当前作品
     * @param perceptualHash 当前作品感知哈希
     * @param now            当前时间
     */
    private boolean runSimilarityCheck(WorkEntity work, String perceptualHash, LocalDateTime now) {
        LambdaQueryWrapper<WorkFeatureEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(WorkFeatureEntity::getWorkId, work.getId())
                .eq(WorkFeatureEntity::getExtractStatus, "SUCCESS")
                .isNotNull(WorkFeatureEntity::getPerceptualHash);

        boolean hasHighRisk = false;
        List<WorkFeatureEntity> existingFeatures = workFeatureMapper.selectList(wrapper);
        for (WorkFeatureEntity existing : existingFeatures) {
            BigDecimal score = calculateSimilarity(perceptualHash, existing.getPerceptualHash());
            String checkResult;
            if (score.compareTo(new BigDecimal("0.9000")) >= 0) {
                checkResult = "HIGH_RISK";
            } else if (score.compareTo(new BigDecimal("0.7000")) >= 0) {
                checkResult = "MANUAL_REVIEW";
            } else {
                checkResult = "PASS";
            }

            WorkSimilarityCheckEntity check = new WorkSimilarityCheckEntity();
            check.setWorkId(work.getId());
            check.setComparedWorkId(existing.getWorkId());
            check.setSimilarityScore(score);
            check.setCheckResult(checkResult);
            check.setCheckTime(now);
            workSimilarityCheckMapper.insert(check);

            if ("HIGH_RISK".equals(checkResult)) {
                hasHighRisk = true;
                log.warn("检测到高相似度作品，workNo={}, comparedWorkId={}, score={}",
                        work.getWorkNo(), existing.getWorkId(), score);
            }
        }
        return hasHighRisk;
    }

    /**
     * 计算两个感知哈希之间的相似度
     * <p>
     * 通过计算哈希字符串的字符差异得出确定性相似度分数（0-1之间）。
     * </p>
     *
     * @param hash1 感知哈希1
     * @param hash2 感知哈希2
     * @return 相似度分数
     */
    private BigDecimal calculateSimilarity(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) {
            return BigDecimal.ZERO;
        }
        int maxLen = Math.max(hash1.length(), hash2.length());
        if (maxLen == 0) {
            return BigDecimal.ONE;
        }
        int minLen = Math.min(hash1.length(), hash2.length());
        int matchCount = 0;
        for (int i = 0; i < minLen; i++) {
            if (hash1.charAt(i) == hash2.charAt(i)) {
                matchCount++;
            }
        }
        return BigDecimal.valueOf(matchCount).divide(BigDecimal.valueOf(maxLen), 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 根据作品状态计算允许的操作列表
     *
     * @param work            作品实体
     * @param viewerAccountId 查看者账户ID
     * @return 允许操作列表
     */
    private List<String> computeAllowedActions(WorkEntity work, Long viewerAccountId) {
        List<String> actions = new ArrayList<>();
        String status = work.getStatus();
        boolean isOwner = viewerAccountId != null && viewerAccountId.equals(work.getCreatorAccountId());

        if (isOwner) {
            if (WorkStatusEnum.DRAFT.getCode().equals(status)
                    || WorkStatusEnum.UPLOADED.getCode().equals(status)) {
                actions.add("UPDATE_META");
            }
            if (WorkStatusEnum.UPLOADED.getCode().equals(status)) {
                actions.add("FEATURE_EXTRACT");
            }
            if (WorkStatusEnum.READY_FOR_CLAIM.getCode().equals(status)) {
                actions.add("SUBMIT_CLAIM");
            }
            if (WorkStatusEnum.OWNERSHIP_CONFIRMED.getCode().equals(status)) {
                actions.add("LIST_FOR_SALE");
            }
        }

        return actions;
    }

    /**
     * 转换为作品文件视图对象
     */
    private WorkFileVO toWorkFileVO(WorkFileEntity entity) {
        WorkFileVO vo = new WorkFileVO();
        vo.setFileId(entity.getId());
        vo.setFileName(entity.getFileName());
        vo.setFileUrl(entity.getFileUrl());
        vo.setFileType(entity.getFileType());
        vo.setFileSize(entity.getFileSize());
        vo.setPurpose(entity.getPurpose());
        return vo;
    }

    /**
     * 转换为AIGC元数据DTO
     */
    private AigcMetaDTO toAigcMetaDTO(WorkAigcMetaEntity entity) {
        AigcMetaDTO dto = new AigcMetaDTO();
        dto.setAigcTool(entity.getAigcTool());
        dto.setAigcModel(entity.getAigcModel());
        dto.setAigcVersion(entity.getAigcVersion());
        dto.setPromptSummary(entity.getPromptSummary());
        dto.setGenerationParams(entity.getGenerationParams());
        if (entity.getGenerationTime() != null) {
            dto.setGenerationTime(DateTimeUtil.formatUtc(entity.getGenerationTime()));
        }
        return dto;
    }

    /**
     * 转换为作品特征视图对象
     */
    private WorkFeatureVO toWorkFeatureVO(WorkFeatureEntity entity) {
        WorkFeatureVO vo = new WorkFeatureVO();
        vo.setFeatureType(entity.getFeatureType());
        vo.setPerceptualHash(entity.getPerceptualHash());
        vo.setExtractStatus(entity.getExtractStatus());
        vo.setExtractTime(entity.getExtractTime());
        return vo;
    }

    /**
     * 转换为作品列表视图对象
     */
    private WorkListVO toWorkListVO(WorkEntity entity) {
        WorkListVO vo = new WorkListVO();
        vo.setWorkNo(entity.getWorkNo());
        vo.setTitle(entity.getTitle());
        vo.setWorkType(entity.getWorkType());
        vo.setCoverUrl(entity.getCoverUrl());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

}
