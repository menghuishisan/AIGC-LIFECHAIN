package com.lifechain.chain.adapter;

import com.lifechain.chain.model.ChainQueryResult;
import com.lifechain.chain.model.ChainSubmitResult;

import java.time.LocalDateTime;

/**
 * DID（数字身份）链码适配器接口
 * <p>
 * 封装 {@code did_chaincode} 链码的全部调用方法。
 * 业务服务通过此适配器完成 DID 的注册、挂起、吊销及查询等链上操作，
 * 无需感知 Fabric SDK 调用细节和链码方法签名。
 * </p>
 *
 * @author LifeChain
 */
public interface DidChainAdapter {

    /**
     * 注册 DID 上链
     *
     * @param bizId       业务ID
     * @param didNo       DID 编号
     * @param didValue    DID 值（W3C DID 格式）
     * @param accountNo   关联账户编号
     * @param subjectType 主体类型（PERSONAL / ENTERPRISE）
     * @param activeTime  生效时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerDid(Long bizId, String didNo, String didValue,
                                  String accountNo, String subjectType, LocalDateTime activeTime);

    /**
     * 挂起 DID 上链
     *
     * @param bizId  业务ID
     * @param didNo  DID 编号
     * @param reason 挂起原因
     * @return 链上提交结果
     */
    ChainSubmitResult suspendDid(Long bizId, String didNo, String reason);

    /**
     * 吊销 DID 上链
     *
     * @param bizId  业务ID
     * @param didNo  DID 编号
     * @param reason 吊销原因
     * @return 链上提交结果
     */
    ChainSubmitResult revokeDid(Long bizId, String didNo, String reason);

    /**
     * 查询链上 DID 信息
     *
     * @param didNo DID 编号
     * @return 链上查询结果
     */
    ChainQueryResult queryDid(String didNo);
}
