package com.lifechain.chain.service;

import com.lifechain.chain.model.ChainQueryResult;
import com.lifechain.chain.model.ChainSubmitRequest;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.chain.receipt.ChainReceiptProcessor;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.chain.record.ChainTxRecordMapper;
import com.lifechain.chain.record.ChainTxRecordService;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.Contract;
import org.hyperledger.fabric.gateway.ContractException;
import org.hyperledger.fabric.gateway.Network;
import org.hyperledger.fabric.gateway.Transaction;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

/**
 * Fabric 链核心服务实现
 * <p>
 * 通过 Fabric Gateway Java SDK 完成交易提交和查询操作。
 * 每次交易提交均自动持久化 {@code chain_tx_record} 记录，确保链上交互可追溯。
 * 所有 Fabric SDK 异常均被捕获并转换为带有标准错误码的业务异常或失败结果。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
public class FabricChainServiceImpl implements FabricChainService {

    private final Network network;
    private final ChainTxRecordService chainTxRecordService;
    private final ChainTxRecordMapper chainTxRecordMapper;
    private final ChainReceiptProcessor chainReceiptProcessor;

    public FabricChainServiceImpl(Network network,
                                  ChainTxRecordService chainTxRecordService,
                                  ChainTxRecordMapper chainTxRecordMapper,
                                  @Lazy ChainReceiptProcessor chainReceiptProcessor) {
        this.network = network;
        this.chainTxRecordService = chainTxRecordService;
        this.chainTxRecordMapper = chainTxRecordMapper;
        this.chainReceiptProcessor = chainReceiptProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult submitTransaction(ChainSubmitRequest request) {
        log.info("开始提交链上交易，bizType={}, bizNo={}, chaincode={}, function={}",
                request.getBizType(), request.getBizNo(),
                request.getChaincodeName(), request.getFunctionName());

        LocalDateTime submitTime = DateTimeUtil.nowUtc();
        ChainSubmitResult result = new ChainSubmitResult();
        result.setSubmitTime(submitTime);
        result.setChannelName(network.getChannel().getName());
        result.setChaincodeName(request.getChaincodeName());

        try {
            Contract contract = network.getContract(request.getChaincodeName());
            Transaction transaction = contract.createTransaction(request.getFunctionName());

            String[] args = request.getArgs() != null ? request.getArgs() : new String[0];
            byte[] response = transaction.submit(args);

            String txId = transaction.getTransactionId();
            String responsePayload = new String(response, StandardCharsets.UTF_8);

            result.setSuccess(true);
            result.setTxHash(txId);
            result.setResponsePayload(responsePayload);
            result.setConfirmTime(DateTimeUtil.nowUtc());
            result.setEndorsementSummary(buildEndorsementSummary(request.getChaincodeName(), txId));

            log.info("链上交易提交成功，bizType={}, bizNo={}, txHash={}",
                    request.getBizType(), request.getBizNo(), txId);

        } catch (ContractException e) {
            log.error("链码执行异常，bizType={}, bizNo={}, 原因={}",
                    request.getBizType(), request.getBizNo(), e.getMessage(), e);
            result.setSuccess(false);
            result.setFailReason("链码执行异常: " + e.getMessage());
            result.setReasonCode(ErrorCodeEnum.CHAIN_SUBMIT_FAILED.getCode());
            result.setConfirmTime(DateTimeUtil.nowUtc());

        } catch (TimeoutException e) {
            log.error("链上交易超时，bizType={}, bizNo={}", request.getBizType(), request.getBizNo(), e);
            result.setSuccess(false);
            result.setFailReason("链上交易提交超时: " + e.getMessage());
            result.setReasonCode(ErrorCodeEnum.CHAIN_RECEIPT_TIMEOUT.getCode());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("链上交易被中断，bizType={}, bizNo={}", request.getBizType(), request.getBizNo(), e);
            result.setSuccess(false);
            result.setFailReason("链上交易被中断");
            result.setReasonCode(ErrorCodeEnum.CHAIN_SUBMIT_FAILED.getCode());

        } catch (Exception e) {
            log.error("链上交易未知异常，bizType={}, bizNo={}", request.getBizType(), request.getBizNo(), e);
            result.setSuccess(false);
            result.setFailReason("链上交易未知异常: " + e.getMessage());
            result.setReasonCode(ErrorCodeEnum.CHAIN_SUBMIT_FAILED.getCode());
        }

        chainTxRecordService.saveRecord(request, result);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainQueryResult queryTransaction(String chaincodeName, String functionName, String... args) {
        log.info("开始查询链上数据，chaincode={}, function={}", chaincodeName, functionName);

        try {
            Contract contract = network.getContract(chaincodeName);
            byte[] response = contract.evaluateTransaction(functionName, args);
            String payload = new String(response, StandardCharsets.UTF_8);

            log.info("链上查询成功，chaincode={}, function={}, 响应长度={}字节",
                    chaincodeName, functionName, response.length);
            return ChainQueryResult.success(payload);

        } catch (ContractException e) {
            log.error("链上查询失败，chaincode={}, function={}, 原因={}",
                    chaincodeName, functionName, e.getMessage(), e);
            return ChainQueryResult.fail("链码查询异常: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChainSubmitResult retrySubmit(Long chainTxRecordId) {
        log.info("开始重试/验证链上交易，recordId={}", chainTxRecordId);

        ChainTxRecordEntity record = chainTxRecordMapper.selectById(chainTxRecordId);
        if (record == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "链上交易记录不存在，recordId=" + chainTxRecordId);
        }

        String currentStatus = record.getChainStatus();
        if (ChainStatusEnum.CHAIN_SUCCESS.getCode().equals(currentStatus)) {
            log.info("链上交易已成功，无需重试，recordId={}, txHash={}", chainTxRecordId, record.getTxHash());
            return buildResultFromRecord(record);
        }

        if (!ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(currentStatus)
                && !ChainStatusEnum.CHAIN_FAILED.getCode().equals(currentStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID,
                    "当前链上状态不可重试，recordId=" + chainTxRecordId + ", status=" + currentStatus,
                    null, currentStatus);
        }

        // 对于有交易哈希的记录，通过系统链码 QSCC 查询交易是否已上链
        if (record.getTxHash() != null && !record.getTxHash().isBlank()) {
            return verifyTransactionOnChain(record);
        }

        // 无交易哈希的记录，说明提交阶段就失败了，无法自动重试（需要业务层重新发起）
        log.warn("链上交易无法自动重试（缺少交易哈希），需业务层重新发起，recordId={}, bizType={}, bizNo={}",
                chainTxRecordId, record.getBizType(), record.getBizNo());
        record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        record.setFailReason("自动重试失败：缺少交易哈希，需业务层重新提交");
        record.setReasonCode(ErrorCodeEnum.CHAIN_SUBMIT_FAILED.getCode());
        chainTxRecordMapper.updateById(record);

        return buildResultFromRecord(record);
    }

    /**
     * 通过系统链码 QSCC 验证交易是否已成功上链
     * <p>
     * 调用 {@code qscc} 系统链码的 {@code GetTransactionByID} 方法，
     * 如果能查到交易说明已成功上链，更新记录状态为 CHAIN_SUCCESS；
     * 如果查不到说明交易未上链，更新记录状态为 CHAIN_FAILED。
     * </p>
     *
     * @param record 链上交易记录
     * @return 验证结果
     */
    private ChainSubmitResult verifyTransactionOnChain(ChainTxRecordEntity record) {
        try {
            Contract qscc = network.getContract("qscc");
            byte[] txData = qscc.evaluateTransaction("GetTransactionByID",
                    record.getChannelName(), record.getTxHash());

            log.info("链上交易验证成功，txHash={}, 响应长度={}字节", record.getTxHash(), txData.length);

            record.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
            record.setConfirmTime(DateTimeUtil.nowUtc());
            record.setFailReason(null);
            record.setReasonCode(null);
            chainTxRecordMapper.updateById(record);

            // 链上成功，通过统一回执处理器推进业务状态
            chainReceiptProcessor.processReceipt(record);

            return buildResultFromRecord(record);

        } catch (ContractException e) {
            log.warn("链上交易验证失败（交易未找到），txHash={}, 原因={}", record.getTxHash(), e.getMessage());

            record.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            record.setFailReason("链上交易验证失败: " + e.getMessage());
            record.setReasonCode(ErrorCodeEnum.CHAIN_RECEIPT_FAILED.getCode());
            chainTxRecordMapper.updateById(record);

            // 链上失败，通过统一回执处理器处理业务失败
            chainReceiptProcessor.processReceipt(record);

            return buildResultFromRecord(record);
        }
    }

    /**
     * 根据交易记录实体构建提交结果对象
     *
     * @param record 链上交易记录实体
     * @return 提交结果
     */
    private ChainSubmitResult buildResultFromRecord(ChainTxRecordEntity record) {
        ChainSubmitResult result = new ChainSubmitResult();
        result.setSuccess(ChainStatusEnum.CHAIN_SUCCESS.getCode().equals(record.getChainStatus()));
        result.setTxHash(record.getTxHash());
        result.setBlockHeight(record.getBlockHeight());
        result.setChannelName(record.getChannelName());
        result.setChaincodeName(record.getChaincodeName());
        result.setEndorsementSummary(record.getEndorsementSummary());
        result.setResponsePayload(record.getResponsePayload());
        result.setFailReason(record.getFailReason());
        result.setReasonCode(record.getReasonCode());
        result.setSubmitTime(record.getSubmitTime());
        result.setConfirmTime(record.getConfirmTime());
        return result;
    }

    /**
     * 构建背书摘要信息
     *
     * @param chaincodeName 链码名称
     * @param txId          交易ID
     * @return 背书摘要字符串
     */
    private String buildEndorsementSummary(String chaincodeName, String txId) {
        return String.format("chaincode=%s, txId=%s, channel=%s",
                chaincodeName, txId, network.getChannel().getName());
    }
}
