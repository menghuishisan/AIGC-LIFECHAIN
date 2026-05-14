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
 * 授权许可链码适配器实现
 * <p>
 * 对接链码 {@code license_chaincode}，将授权许可的关键信息（授权双方DID、授权类型、合同哈希等）
 * 序列化并提交到 Fabric 网络，实现授权关系的链上不可篡改存证。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseChainAdapterImpl implements LicenseChainAdapter {

    private static final String CHAINCODE_NAME = "license_chaincode";

    private final FabricChainService fabricChainService;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult registerLicense(Long bizId, String licenseNo, String workNo,
                                             String licensorDid, String licenseeDid,
                                             String licenseType, String licenseHash,
                                             LocalDateTime effectiveTime) {
        log.info("授权许可上链，bizId={}, licenseNo={}, workNo={}", bizId, licenseNo, workNo);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("licenseNo", licenseNo);
        payload.put("workNo", workNo);
        payload.put("licensorDid", licensorDid);
        payload.put("licenseeDid", licenseeDid);
        payload.put("licenseType", licenseType);
        payload.put("licenseHash", licenseHash);
        payload.put("effectiveTime", DateTimeUtil.formatUtc(effectiveTime));

        String payloadJson = serializePayload(payload);

        ChainSubmitRequest request = new ChainSubmitRequest();
        request.setBizType(BizTypeEnum.LICENSE.getCode());
        request.setBizId(bizId);
        request.setBizNo(licenseNo);
        request.setTxType("REGISTER");
        request.setChaincodeName(CHAINCODE_NAME);
        request.setFunctionName("RegisterLicense");
        request.setArgs(new String[]{payloadJson});
        request.setRequestPayloadHash(HashUtil.sha256(payloadJson));
        request.setIdempotentKey(BizTypeEnum.LICENSE.getCode() + ":" + bizId + ":REGISTER");

        return fabricChainService.submitTransaction(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainQueryResult queryLicense(String licenseNo) {
        log.info("查询链上授权信息，licenseNo={}", licenseNo);
        return fabricChainService.queryTransaction(CHAINCODE_NAME, "QueryLicense", licenseNo);
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
