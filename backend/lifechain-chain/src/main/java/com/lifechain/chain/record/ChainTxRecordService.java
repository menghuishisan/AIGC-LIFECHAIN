package com.lifechain.chain.record;

import com.lifechain.chain.model.ChainSubmitRequest;
import com.lifechain.chain.model.ChainSubmitResult;

import java.util.List;

/**
 * 区块链交易记录服务接口
 * <p>
 * 提供链上交易记录的持久化和查询能力，供链核心服务和补偿任务使用。
 * </p>
 *
 * @author LifeChain
 */
public interface ChainTxRecordService {

    /**
     * 保存链上交易记录
     * <p>
     * 根据提交请求和提交结果，构建并持久化一条完整的链上交易记录。
     * </p>
     *
     * @param request 链上交易提交请求
     * @param result  链上交易提交结果
     */
    void saveRecord(ChainSubmitRequest request, ChainSubmitResult result);

    /**
     * 根据业务类型和业务编号查询链上交易记录
     *
     * @param bizType 业务类型编码
     * @param bizNo   业务编号
     * @return 匹配的交易记录，不存在返回 null
     */
    ChainTxRecordEntity getByBizTypeAndBizNo(String bizType, String bizNo);

    /**
     * 根据交易哈希查询链上交易记录
     *
     * @param txHash Fabric 交易哈希
     * @return 匹配的交易记录，不存在返回 null
     */
    ChainTxRecordEntity getByTxHash(String txHash);

    /**
     * 根据业务类型和业务ID查询链上交易记录列表
     * <p>
     * 同一业务实体可能有多次链上操作（如注册、更新、冻结等），
     * 此方法返回该业务实体的全部链上交易记录。
     * </p>
     *
     * @param bizType 业务类型编码
     * @param bizId   业务ID
     * @return 交易记录列表，无记录时返回空列表
     */
    List<ChainTxRecordEntity> listByBizTypeAndBizId(String bizType, Long bizId);

    /**
     * 根据业务类型和业务编号查询链上交易记录列表
     *
     * @param bizType 业务类型编码
     * @param bizNo   业务编号
     * @return 交易记录列表，无记录时返回空列表
     */
    List<ChainTxRecordEntity> listByBizTypeAndBizNo(String bizType, String bizNo);

}
