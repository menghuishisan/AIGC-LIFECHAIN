package com.lifechain.work.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 验真结果视图对象
 * <p>
 * 返回验真查询的结果信息，根据查询来源不同（PUBLIC/LOGIN/REGULATOR）
 * 返回不同级别的详细信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
public class VerifyResultVO implements Serializable {

    /** 是否验证通过 */
    private boolean verified;

    /** 证书编号 */
    private String certNo;

    /** 作品编号 */
    private String workNo;

    /** 创作者DID */
    private String creatorDid;

    /** 确权时间 */
    private String claimTime;

    /** 证书状态 */
    private String certStatus;

    /** 链上交易哈希 */
    private String chainTxHash;

    /** 区块高度 */
    private String blockHeight;

    /** 验证级别（PUBLIC/LOGIN/REGULATOR） */
    private String verifyLevel;

    /** 确权摘要哈希 */
    private String summaryHash;
}
