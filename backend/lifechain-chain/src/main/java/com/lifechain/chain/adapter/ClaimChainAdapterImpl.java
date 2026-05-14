package com.lifechain.chain.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifechain.chain.model.ChainQueryResult;
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
 * 确权申请链码适配器实现
 * <p>
 * 对接链码 {@code claim_chaincode}，负责将确权申请的核心信息（包括文件哈希、元数据哈希等）
 * 序列化后提交到 Fabric 网络，实现确权信息的链上永久存证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimChainAdapterImpl implements ClaimChainAdapter {

    private static final String CHAINCODE_NAME = "claim_chaincode";

    private final FabricChainService fabricChainService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerClaim(Long bizId, String claimNo, String workNo,
                                           String creatorDid, String fileHash, String metaHash,
                                           String summaryHash, LocalDateTime claimTime) {
        log.info("确权申请上链，bizId={}, claimNo={}, workNo={}", bizId, claimNo, workNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("claimNo", claimNo);
        payload.put("workNo", workNo);
        payload.put("creatorDid", creatorDid);
        payload.put("fileHash", fileHash);
        payload.put("metaHash", metaHash);
        payload.put("summaryHash", summaryHash);
        payload.put("claimTime", DateTimeUtil.formatUtc(claimTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.CLAIM.getCode());
        request.setBizId(bizId);
        request.setBizNo(claimNo);
        request.setTxType("REGISTER");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterClaim");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.CLAIM.getCode() + ":" + bizId + ":REGISTER");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainQueryResult queryClaim(String claimNo) {
        log.info("查询链上确权信息，claimNo={}", claimNo);
        return fabricChainService.queryTransaction(CHAINCODE_NAME, "QueryClaim", claimNo);
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
