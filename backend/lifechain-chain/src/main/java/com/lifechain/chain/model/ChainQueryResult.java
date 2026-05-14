package com.lifechain.chain.model;

import lombok.Data;

/**
 * 链上查询结果
 * <p>
 * 封装 Fabric 链码查询（evaluateTransaction）的返回结果。
 * 查询操作不产生交易，不写入账本，仅从 Peer 节点本地状态数据库读取。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ChainQueryResult {

    /** 是否成功 */
    private boolean success;

    /** 查询结果 JSON */
    private String payload;

    /** 失败原因 */
    private String failReason;

    /**
     * 构建成功的查询结果
     *
     * @param payload 查询返回的 JSON 负载
     * @return 成功结果
     */
    public static ChainQueryResult success(String payload) {
        ChainQueryResult result = new ChainQueryResult();
        result.setSuccess(true);
        result.setPayload(payload);
        return result;
    }

    /**
     * 构建失败的查询结果
     *
     * @param failReason 失败原因描述
     * @return 失败结果
     */
    public static ChainQueryResult fail(String failReason) {
        ChainQueryResult result = new ChainQueryResult();
        result.setSuccess(false);
        result.setFailReason(failReason);
        return result;
    }
}
