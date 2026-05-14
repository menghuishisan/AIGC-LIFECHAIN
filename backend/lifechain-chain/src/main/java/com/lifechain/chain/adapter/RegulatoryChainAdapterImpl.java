package com.lifechain.chain.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifechain.chain.model.ChainSubmitRequest;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.chain.service.FabricChainService;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监管链码适配器实现
 * <p>
 * 对接链码 {@code regulatory_chaincode}，将冻结/解冻、争议结论、监管报告等
 * 监管行为信息序列化后提交到 Fabric 网络，实现监管执法全过程的链上不可篡改存证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegulatoryChainAdapterImpl implements RegulatoryChainAdapter {

    private static final String CHAINCODE_NAME = "regulatory_chaincode";

    private final FabricChainService fabricChainService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerFreeze(Long bizId, String freezeNo, String targetType,
                                            String targetNo, String reason, LocalDateTime freezeTime) {
        log.info("冻结操作上链，bizId={}, freezeNo={}, targetType={}, targetNo={}",
                bizId, freezeNo, targetType, targetNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("freezeNo", freezeNo);
        payload.put("targetType", targetType);
        payload.put("targetNo", targetNo);
        payload.put("reason", reason);
        payload.put("freezeTime", DateTimeUtil.formatUtc(freezeTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.FREEZE.getCode());
        request.setBizId(bizId);
        request.setBizNo(freezeNo);
        request.setTxType("REGISTER");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterFreeze");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.FREEZE.getCode() + ":" + bizId + ":REGISTER");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerUnfreeze(Long bizId, String freezeNo, String reason,
                                              LocalDateTime unfreezeTime) {
        log.info("解冻操作上链，bizId={}, freezeNo={}", bizId, freezeNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("freezeNo", freezeNo);
        payload.put("reason", reason);
        payload.put("unfreezeTime", DateTimeUtil.formatUtc(unfreezeTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.FREEZE.getCode());
        request.setBizId(bizId);
        request.setBizNo(freezeNo);
        request.setTxType("UPDATE");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterUnfreeze");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.FREEZE.getCode() + ":" + bizId + ":UNFREEZE");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerDisputeConclusion(Long bizId, String caseNo, String conclusion,
                                                       String resultSummary, LocalDateTime closeTime) {
        log.info("争议结论上链，bizId={}, caseNo={}, conclusion={}", bizId, caseNo, conclusion);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("caseNo", caseNo);
        payload.put("conclusion", conclusion);
        payload.put("resultSummary", resultSummary);
        payload.put("closeTime", DateTimeUtil.formatUtc(closeTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.DISPUTE.getCode());
        request.setBizId(bizId);
        request.setBizNo(caseNo);
        request.setTxType("UPDATE");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterDisputeConclusion");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.DISPUTE.getCode() + ":" + bizId + ":CONCLUSION");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerReport(Long bizId, String reportNo, String summaryHash,
                                            LocalDateTime generateTime) {
        log.info("监管报告上链，bizId={}, reportNo={}", bizId, reportNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reportNo", reportNo);
        payload.put("summaryHash", summaryHash);
        payload.put("generateTime", DateTimeUtil.formatUtc(generateTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.REPORT.getCode());
        request.setBizId(bizId);
        request.setBizNo(reportNo);
        request.setTxType("REGISTER");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterReport");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.REPORT.getCode() + ":" + bizId + ":REGISTER");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * 将参数 Map 序列化为 JSON 字符串
     *
     * @param payload 参数键值对
     * @return JSON 字符串
     */
    private String serializePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("链码参数序列化失败，payload={}", payload, e);
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "链码参数序列化失败: " + e.getMessage());
        }
    }
}
