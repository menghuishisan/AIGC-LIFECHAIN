package com.lifechain.chain.adapter;

import com.lifechain.chain.model.ChainQueryResult;
import com.lifechain.chain.model.ChainSubmitResult;

import java.time.LocalDateTime;

/**
 * 确权申请链码适配器接口
 * <p>
 * 封装 {@code claim_chaincode} 链码的全部调用方法。
 * 确权申请上链后不可篡改，确保作品确权过程的完整存证。
 * </p>
 *
 * @author LifeChain
 */
public interface ClaimChainAdapter {

    /**
     * 确权申请上链
     *
     * @param bizId       业务ID
     * @param claimNo     确权申请编号
     * @param workNo      作品编号
     * @param creatorDid  创作者DID
     * @param fileHash    作品文件哈希（SHA-256）
     * @param metaHash    元数据哈希
     * @param summaryHash 摘要哈希
     * @param claimTime   确权时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerClaim(Long bizId, String claimNo, String workNo,
                                    String creatorDid, String fileHash, String metaHash,
                                    String summaryHash, LocalDateTime claimTime);

    /**
     * 查询链上确权信息
     *
     * @param claimNo 确权申请编号
     * @return 链上查询结果
     */
    ChainQueryResult queryClaim(String claimNo);
}
