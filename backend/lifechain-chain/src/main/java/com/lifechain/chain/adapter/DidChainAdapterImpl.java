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
 * DID（数字身份）链码适配器实现
 * <p>
 * 对接链码 {@code did_chaincode}，将业务参数序列化为链码所需的 JSON 格式，
 * 通过 {@link FabricChainService} 提交到 Fabric 网络。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DidChainAdapterImpl implements DidChainAdapter {

    private static final String CHAINCODE_NAME = "did_chaincode";

    private final FabricChainService fabricChainService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerDid(Long bizId, String didNo, String didValue,
                                         String accountNo, String subjectType, LocalDateTime activeTime) {
        log.info("DID注册上链，bizId={}, didNo={}", bizId, didNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("didNo", didNo);
        payload.put("didValue", didValue);
        payload.put("accountNo", accountNo);
        payload.put("subjectType", subjectType);
        payload.put("activeTime", DateTimeUtil.formatUtc(activeTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.DID.getCode());
        request.setBizId(bizId);
        request.setBizNo(didNo);
        request.setTxType("REGISTER");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterDID");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.DID.getCode() + ":" + bizId + ":REGISTER");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult suspendDid(Long bizId, String didNo, String reason) {
        log.info("DID挂起上链，bizId={}, didNo={}", bizId, didNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("didNo", didNo);
        payload.put("reason", reason);
        payload.put("suspendTime", DateTimeUtil.formatUtc(DateTimeUtil.nowUtc()));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.DID.getCode());
        request.setBizId(bizId);
        request.setBizNo(didNo);
        request.setTxType("UPDATE");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("SuspendDID");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.DID.getCode() + ":" + bizId + ":SUSPEND");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult revokeDid(Long bizId, String didNo, String reason) {
        log.info("DID吊销上链，bizId={}, didNo={}", bizId, didNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("didNo", didNo);
        payload.put("reason", reason);
        payload.put("revokeTime", DateTimeUtil.formatUtc(DateTimeUtil.nowUtc()));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.DID.getCode());
        request.setBizId(bizId);
        request.setBizNo(didNo);
        request.setTxType("UPDATE");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RevokeDID");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.DID.getCode() + ":" + bizId + ":REVOKE");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainQueryResult queryDid(String didNo) {
        log.info("查询链上DID，didNo={}", didNo);
        return fabricChainService.queryTransaction(CHAINCODE_NAME, "QueryDID", didNo);
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
