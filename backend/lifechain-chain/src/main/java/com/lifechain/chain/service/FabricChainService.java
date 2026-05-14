package com.lifechain.chain.service;

import com.lifechain.chain.model.ChainQueryResult;
import com.lifechain.chain.model.ChainSubmitRequest;
import com.lifechain.chain.model.ChainSubmitResult;

/**
 * Fabric 链核心服务接口
 * <p>
 * 封装 Hyperledger Fabric Gateway 的交易提交与查询操作。
 * 所有业务模块通过此接口（或链码适配器）与区块链交互，禁止直接使用 Fabric SDK。
 * </p>
 *
 * @author LifeChain
 */
public interface FabricChainService {

    /**
     * 提交交易到 Fabric 网络
     * <p>
     * 执行链码写操作，交易经过背书→排序→验证→提交的完整共识流程。
     * 提交结果包含交易哈希和区块信息，同时自动持久化链上交易记录。
     * </p>
     *
     * @param request 链上交易提交请求
     * @return 交易提交结果
     */
    ChainSubmitResult submitTransaction(ChainSubmitRequest request);

    /**
     * 查询链上数据
     * <p>
     * 执行链码只读操作（evaluateTransaction），仅在背书节点本地执行，
     * 不产生交易，不写入账本，无需排序共识。
     * </p>
     *
     * @param chaincodeName 链码名称
     * @param functionName  链码方法名
     * @param args          链码参数
     * @return 查询结果
     */
    ChainQueryResult queryTransaction(String chaincodeName, String functionName, String... args);

    /**
     * 重试/验证失败或超时的链上交易
     * <p>
     * 对于已提交但未收到回执的交易（CHAIN_SUBMITTED），尝试通过系统链码查询交易状态；
     * 若交易已上链则更新记录为成功，否则根据超时策略标记失败。
     * </p>
     *
     * @param chainTxRecordId 链上交易记录ID
     * @return 重试/验证结果
     */
    ChainSubmitResult retrySubmit(Long chainTxRecordId);
}
