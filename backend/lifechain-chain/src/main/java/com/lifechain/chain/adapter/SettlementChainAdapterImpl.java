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
 * 结算链码适配器实现
 * <p>
 * 对接链码 {@code settlement_chaincode}，将分账结算和逆分账的关键信息
 * 序列化后提交到 Fabric 网络，实现资金流转的链上不可篡改存证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementChainAdapterImpl implements SettlementChainAdapter {

    private static final String CHAINCODE_NAME = "settlement_chaincode";

    private final FabricChainService fabricChainService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerSettlement(Long bizId, String settleNo, String orderNo,
                                                Long totalAmount, String summaryHash,
                                                LocalDateTime settleTime) {
        log.info("结算记录上链，bizId={}, settleNo={}, orderNo={}", bizId, settleNo, orderNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("settleNo", settleNo);
        payload.put("orderNo", orderNo);
        payload.put("totalAmount", totalAmount);
        payload.put("summaryHash", summaryHash);
        payload.put("settleTime", DateTimeUtil.formatUtc(settleTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.SETTLEMENT.getCode());
        request.setBizId(bizId);
        request.setBizNo(settleNo);
        request.setTxType("REGISTER");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterSettlement");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.SETTLEMENT.getCode() + ":" + bizId + ":REGISTER");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerReverseSettlement(Long bizId, String reverseNo, String settleNo,
                                                       Long reverseAmount, String reason,
                                                       LocalDateTime reverseTime) {
        log.info("逆分账记录上链，bizId={}, reverseNo={}, settleNo={}", bizId, reverseNo, settleNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reverseNo", reverseNo);
        payload.put("settleNo", settleNo);
        payload.put("reverseAmount", reverseAmount);
        payload.put("reason", reason);
        payload.put("reverseTime", DateTimeUtil.formatUtc(reverseTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.REVERSE_SETTLEMENT.getCode());
        request.setBizId(bizId);
        request.setBizNo(reverseNo);
        request.setTxType("REGISTER");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterReverseSettlement");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.REVERSE_SETTLEMENT.getCode() + ":" + bizId + ":REGISTER");

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
