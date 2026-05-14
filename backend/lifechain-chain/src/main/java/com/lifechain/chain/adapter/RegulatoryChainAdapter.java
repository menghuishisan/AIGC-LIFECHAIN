package com.lifechain.chain.adapter;

import com.lifechain.chain.model.ChainSubmitResult;

import java.time.LocalDateTime;

/**
 * 监管链码适配器接口
 * <p>
 * 封装 {@code regulatory_chaincode} 链码的全部调用方法。
 * 涵盖冻结/解冻、争议结论、监管报告等监管域业务的链上存证操作。
 * 所有监管行为一旦上链即不可篡改，确保监管执法的公信力和可追溯性。
 * </p>
 *
 * @author LifeChain
 */
public interface RegulatoryChainAdapter {

    /**
     * 冻结操作上链
     *
     * @param bizId      业务ID
     * @param freezeNo   冻结编号
     * @param targetType 冻结目标类型（ACCOUNT / WORK / ORDER 等）
     * @param targetNo   冻结目标编号
     * @param reason     冻结原因
     * @param freezeTime 冻结时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerFreeze(Long bizId, String freezeNo, String targetType,
                                     String targetNo, String reason, LocalDateTime freezeTime);

    /**
     * 解冻操作上链
     *
     * @param bizId        业务ID
     * @param freezeNo     冻结编号
     * @param reason       解冻原因
     * @param unfreezeTime 解冻时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerUnfreeze(Long bizId, String freezeNo, String reason,
                                       LocalDateTime unfreezeTime);

    /**
     * 争议结论上链
     *
     * @param bizId         业务ID
     * @param caseNo        争议案件编号
     * @param conclusion    结论（如：COMPLAINANT_WIN / RESPONDENT_WIN / DISMISSED 等）
     * @param resultSummary 结果摘要
     * @param closeTime     争议关闭时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerDisputeConclusion(Long bizId, String caseNo, String conclusion,
                                                String resultSummary, LocalDateTime closeTime);

    /**
     * 监管报告上链
     *
     * @param bizId        业务ID
     * @param reportNo     报告编号
     * @param summaryHash  报告摘要哈希
     * @param generateTime 报告生成时间（UTC）
     * @return 链上提交结果
     */
    ChainSubmitResult registerReport(Long bizId, String reportNo, String summaryHash,
                                     LocalDateTime generateTime);
}
