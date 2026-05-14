package com.lifechain.chain.adapter;

import com.lifechain.chain.model.ChainQueryResult;
import com.lifechain.chain.model.ChainSubmitResult;

import java.time.LocalDateTime;

/**
 * 授权许可链码适配器接口
 * <p>
 * 封装 {@code license_chaincode} 链码的全部调用方法。
 * 授权信息上链后，授权方和被授权方均可通过链上记录验证授权有效性。
 * </p>
 *
 * @author LifeChain
 */
public interface LicenseChainAdapter {

    /**
     * 授权许可上链
     *
     * @param bizId         业务ID
     * @param licenseNo     授权编号
     * @param workNo        作品编号
     * @param licensorDid   授权方DID
     * @param licenseeDid   被授权方DID
     * @param licenseType   授权类型
     * @param licenseHash   授权合同哈希
     * @param effectiveTime 生效时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerLicense(Long bizId, String licenseNo, String workNo,
                                      String licensorDid, String licenseeDid,
                                      String licenseType, String licenseHash,
                                      LocalDateTime effectiveTime);

    /**
     * 查询链上授权信息
     *
     * @param licenseNo 授权编号
     * @return 链上查询结果
     */
    ChainQueryResult queryLicense(String licenseNo);
}
