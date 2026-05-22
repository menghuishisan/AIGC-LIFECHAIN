package com.lifechain.app.consumer;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.WorkStatusEnum;
import com.lifechain.common.mq.FeatureExtractMessage;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.infra.milvus.MilvusService;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.lifechain.work.entity.WorkAigcMetaEntity;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.entity.WorkFeatureEntity;
import com.lifechain.work.entity.WorkSimilarityCheckEntity;
import com.lifechain.work.mapper.WorkAigcMetaMapper;
import com.lifechain.work.mapper.WorkFeatureMapper;
import com.lifechain.work.mapper.WorkMapper;
import com.lifechain.work.mapper.WorkSimilarityCheckMapper;
import com.lifechain.work.service.FeatureExtractService;
import com.rabbitmq.client.Channel;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 特征提取消息消费者
 * <p>
 * 消费特征提取消息，调用 Python 特征提取服务获取 256-bit 二进制感知指纹（hex 64 字符），
 * 写入 work_feature 表和 Milvus 向量库（按作品类型路由到对应的 BinaryVector collection），
 * 通过 HAMMING 距离执行 ANN 相似度检测，根据检测结果更新作品状态为
 * READY_FOR_CLAIM 或 SIMILARITY_HIGH_RISK。
 * </p>
 * <p>
 * 同源 AIGC 生成识别：若候选作品的 generation_fingerprint
 * （model + version + 规范化 prompt + seed 的 sha256）与待检作品一致，
 * 视为同 AI 同 prompt 生成，自动降级为 PASS（不视为抄袭）。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureExtractConsumer {

    private final FeatureExtractService featureExtractService;
    private final MilvusService milvusService;
    private final WorkMapper workMapper;
    private final WorkFeatureMapper workFeatureMapper;
    private final WorkAigcMetaMapper workAigcMetaMapper;
    private final WorkSimilarityCheckMapper workSimilarityCheckMapper;
    private final AuditService auditService;
    private final TraceEventService traceEventService;

    /** 高风险阈值：score ≥ 0.88（汉明距离 ≤ 30，PDQ 业界通用阈值） */
    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("0.8800");
    /** 人工复核阈值：score ≥ 0.80（汉明距离 ≤ 51） */
    private static final BigDecimal MANUAL_REVIEW_THRESHOLD = new BigDecimal("0.8000");
    /** ANN 检索 Top-K */
    private static final int TOP_K = 10;

    /**
     * 处理特征提取消息
     * <p>
     * 调用 Python 特征服务 → 写入特征记录 → Milvus 向量入库 → 相似度检测 → 更新作品状态。
     * 失败时回退作品状态并记录失败原因。
     * </p>
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_FEATURE_EXTRACT)
    public void handle(FeatureExtractMessage message, Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到特征提取消息: workNo={}, workType={}", message.getWorkNo(), message.getWorkType());
        try {
            processFeatureExtract(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("特征提取失败: workNo={}", message.getWorkNo(), e);
            handleFailure(message, e.getMessage());
            channel.basicAck(deliveryTag, false);
        }
    }

    /** 执行完整的特征提取流程 */
    private void processFeatureExtract(FeatureExtractMessage message) {
        LocalDateTime now = DateTimeUtil.nowUtc();

        // 1. 调用 Python 特征提取服务，获得 256-bit 二进制指纹（hex 编码）
        FeatureExtractService.FeatureResult result = featureExtractService.extract(
                message.getWorkType(), message.getFilePath());

        // 2. 计算 AIGC 来源指纹（用于识别同源 AI 生成，避免误报）
        String generationFingerprint = computeGenerationFingerprint(message.getWorkId());

        // 3. 持久化特征记录到 work_feature 表
        WorkFeatureEntity feature = new WorkFeatureEntity();
        feature.setWorkId(message.getWorkId());
        feature.setWorkType(result.workType());
        feature.setAlgo(result.algo());
        feature.setAlgoVersion(result.algoVersion());
        feature.setVectorDim(MilvusService.VECTOR_BITS);
        feature.setPerceptualHash(result.perceptualHash());
        feature.setGenerationFingerprint(generationFingerprint);
        feature.setExtractStatus("SUCCESS");
        feature.setExtractTime(now);
        feature.setExtra(result.extra());
        workFeatureMapper.insert(feature);

        // 4. 写入 Milvus 二进制向量库（按作品类型路由到对应 collection，用于后续 HAMMING ANN 检索）
        milvusService.insert(message.getWorkId(), message.getWorkType(), result.perceptualHash());

        // 5. 执行相似度检测：搜索同类型 collection 的 Top-K，按汉明距离换算分数
        boolean highRisk = runSimilarityCheck(message, result, generationFingerprint, now);

        // 6. 根据检测结果更新作品状态
        WorkEntity work = workMapper.selectById(message.getWorkId());
        String newStatus = highRisk
                ? WorkStatusEnum.SIMILARITY_HIGH_RISK.getCode()
                : WorkStatusEnum.READY_FOR_CLAIM.getCode();

        work.setStatus(newStatus);
        workMapper.updateById(work);

        // 7. 记录审计日志和轨迹
        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                WorkStatusEnum.FEATURE_PENDING.getCode(), newStatus,
                highRisk ? "特征提取完成，检测到高相似度" : "特征提取完成", null, message.getAccountId());

        auditService.writeAuditLog(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "FEATURE_EXTRACT",
                "特征提取完成，algo=" + result.algo() + " hash=" + result.perceptualHash(),
                message.getAccountId(), null, null, "SUCCESS", null);

        traceEventService.writeTraceEvent(BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                "WORK_FEATURE_EXTRACTED", "特征提取完成", message.getAccountId(), null, null);

        log.info("特征提取流程完成: workNo={}, status={}", message.getWorkNo(), newStatus);
    }

    /**
     * 通过 Milvus 同类型 collection 进行 HAMMING ANN 搜索，并按阈值持久化检测结果
     *
     * @param message               原始消息
     * @param result                Python 服务返回的指纹
     * @param myFingerprint         待检作品的 AIGC 来源指纹（用于排除同源生成）
     * @param now                   检测时间
     * @return 是否存在高风险候选
     */
    private boolean runSimilarityCheck(FeatureExtractMessage message,
                                       FeatureExtractService.FeatureResult result,
                                       String myFingerprint,
                                       LocalDateTime now) {
        // 向量近邻搜索：同类型 collection、Top-K
        List<SearchResp.SearchResult> candidates = milvusService.searchSimilar(
                message.getWorkType(), result.perceptualHash(), TOP_K);

        boolean hasHighRisk = false;
        for (SearchResp.SearchResult candidate : candidates) {
            Long comparedWorkId = (Long) candidate.getEntity().get("work_id");
            // 排除自身
            if (comparedWorkId.equals(message.getWorkId())) continue;

            // Milvus HAMMING 距离（0~256）转换为相似度分数：score = 1 - distance / 256
            int hammingDistance = Math.round(candidate.getDistance());
            BigDecimal score = BigDecimal.valueOf(1.0 - (double) hammingDistance / MilvusService.VECTOR_BITS)
                    .setScale(4, RoundingMode.HALF_UP);

            // 同源 AIGC 生成识别：若双方 generation_fingerprint 一致，视为同 AI 同 prompt 复现，强制降级为 PASS
            boolean sameGeneration = myFingerprint != null
                    && myFingerprint.equals(loadGenerationFingerprint(comparedWorkId));

            // 阈值判定：HIGH_RISK ≥ 0.88、MANUAL_REVIEW ≥ 0.80、其余 PASS
            String checkResult;
            if (sameGeneration) {
                checkResult = "PASS";
            } else if (score.compareTo(HIGH_RISK_THRESHOLD) >= 0) {
                checkResult = "HIGH_RISK";
                hasHighRisk = true;
            } else if (score.compareTo(MANUAL_REVIEW_THRESHOLD) >= 0) {
                checkResult = "MANUAL_REVIEW";
            } else {
                checkResult = "PASS";
            }

            // 持久化检测记录
            WorkSimilarityCheckEntity check = new WorkSimilarityCheckEntity();
            check.setWorkId(message.getWorkId());
            check.setComparedWorkId(comparedWorkId);
            check.setAlgo(result.algo());
            check.setHammingDistance(hammingDistance);
            check.setSimilarityScore(score);
            check.setCheckResult(checkResult);
            check.setCheckTime(now);
            workSimilarityCheckMapper.insert(check);
        }
        return hasHighRisk;
    }

    /**
     * 计算作品的 AIGC 来源指纹
     * <p>
     * 公式：sha256(aigc_model + "|" + aigc_version + "|" + 规范化 prompt + "|" + seed)。
     * prompt 规范化：去除前后空白、多个空白合并为单个空格、统一小写。
     * 若作品无 AIGC 元数据（非 AI 生成）或缺关键字段，返回 null（不参与同源识别）。
     * </p>
     */
    private String computeGenerationFingerprint(Long workId) {
        WorkAigcMetaEntity meta = workAigcMetaMapper.selectByWorkId(workId);
        if (meta == null || meta.getAigcModel() == null || meta.getPromptSummary() == null) {
            return null;
        }
        String model = meta.getAigcModel();
        String version = meta.getAigcVersion() != null ? meta.getAigcVersion() : "";
        String prompt = meta.getPromptSummary().trim().replaceAll("\\s+", " ").toLowerCase();
        // 从 generation_params JSON 中粗略抽取 seed（避免引入 JSON 解析依赖：直接子串提取）
        String seed = extractSeed(meta.getGenerationParams());
        return HashUtil.sha256(model + "|" + version + "|" + prompt + "|" + seed);
    }

    /** 加载候选作品的 AIGC 来源指纹（用于同源识别比对） */
    private String loadGenerationFingerprint(Long comparedWorkId) {
        WorkFeatureEntity feature = workFeatureMapper.selectByWorkId(comparedWorkId);
        return feature != null ? feature.getGenerationFingerprint() : null;
    }

    /** 从 generation_params JSON 字符串中粗略抽取 seed 字段值 */
    private String extractSeed(String generationParams) {
        if (generationParams == null) return "";
        int idx = generationParams.indexOf("\"seed\"");
        if (idx < 0) return "";
        int colon = generationParams.indexOf(":", idx);
        if (colon < 0) return "";
        int end = colon + 1;
        // 跳过空白
        while (end < generationParams.length() && Character.isWhitespace(generationParams.charAt(end))) end++;
        int start = end;
        // 截取数字或字符串值（停在 , } " 处）
        while (end < generationParams.length()) {
            char c = generationParams.charAt(end);
            if (c == ',' || c == '}' || c == '"') break;
            end++;
        }
        return generationParams.substring(start, end).trim();
    }

    /** 特征提取失败时回退作品状态并记录失败原因 */
    private void handleFailure(FeatureExtractMessage message, String reason) {
        WorkEntity work = workMapper.selectById(message.getWorkId());
        if (work == null) return;

        WorkFeatureEntity feature = new WorkFeatureEntity();
        feature.setWorkId(message.getWorkId());
        feature.setWorkType(message.getWorkType());
        feature.setAlgo("UNKNOWN");
        feature.setAlgoVersion("0");
        feature.setVectorDim(MilvusService.VECTOR_BITS);
        feature.setPerceptualHash("");
        feature.setExtractStatus("FAILED");
        feature.setFailReason(reason != null ? reason.substring(0, Math.min(reason.length(), 500)) : "未知错误");
        feature.setExtractTime(DateTimeUtil.nowUtc());
        workFeatureMapper.insert(feature);

        work.setStatus(WorkStatusEnum.UPLOADED.getCode());
        workMapper.updateById(work);

        auditService.writeStatusHistory(
                BizTypeEnum.WORK.getCode(), work.getId(), work.getWorkNo(),
                WorkStatusEnum.FEATURE_PENDING.getCode(), WorkStatusEnum.UPLOADED.getCode(),
                "特征提取失败，回退状态", null, message.getAccountId());
    }
}
