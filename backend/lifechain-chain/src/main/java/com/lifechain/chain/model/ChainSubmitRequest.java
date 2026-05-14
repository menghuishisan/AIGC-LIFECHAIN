package com.lifechain.chain.model;

import lombok.Data;

/**
 * 链上交易提交请求
 * <p>
 * 封装一次 Fabric 链码调用所需的全部参数，由链码适配器构建后交给核心链服务提交。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ChainSubmitRequest {

    /** 业务类型（对应 BizTypeEnum） */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 交易类型：REGISTER / UPDATE / QUERY */
    private String txType;

    /** 链码名称 */
    private String chaincodeName;

    /** 链码方法名 */
    private String functionName;

    /** 链码参数数组 */
    private String[] args;

    /** 请求负载哈希（SHA-256），用于防篡改校验 */
    private String requestPayloadHash;

    /** 幂等键，防止重复提交 */
    private String idempotentKey;
}
