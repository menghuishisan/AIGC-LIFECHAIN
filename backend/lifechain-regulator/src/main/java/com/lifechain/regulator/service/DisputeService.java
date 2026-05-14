package com.lifechain.regulator.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.CreateDisputeRequest;
import com.lifechain.regulator.dto.DisputeCaseVO;
import com.lifechain.regulator.dto.DisputeProcessRequest;
import com.lifechain.regulator.dto.RegulatorDisputeListVO;
import com.lifechain.regulator.dto.RegulatorDisputeQuery;

/**
 * 争议案件服务接口
 * <p>
 * 提供争议案件完整生命周期管理：创建、补充证据、处理流转、查询。
 * 争议案件涉及双方当事人，状态按照争议处理流程依次流转。
 * 关键节点（创建、结案）的结论上链存证，确保争议处理结果的公信力和不可篡改性。
 * 所有状态变更均记录审计日志、状态变更历史和处理记录。
 * </p>
 *
 * @author LifeChain
 */
public interface DisputeService {

    /**
     * 创建争议案件
     * <p>
     * 申请人发起争议，生成案件编号，创建初始处理记录"案件创建"。
     * 初始状态为DISPUTE_SUBMITTED，同步写入审计日志和状态变更历史。
     * </p>
     *
     * @param applicantAccountId 申请人账户ID
     * @param request            创建争议请求
     * @return 争议案件视图对象
     */
    DisputeCaseVO createDispute(Long applicantAccountId, CreateDisputeRequest request);

    /**
     * 补充证据
     * <p>
     * 校验案件存在且未关闭，创建证据记录（含文件哈希），添加处理记录"提交证据"。
     * </p>
     *
     * @param submitterAccountId 提交人账户ID
     * @param caseNo             案件编号
     * @param evidenceType       证据类型
     * @param fileUrl            文件地址
     * @param description        证据描述
     * @param fileHash           文件内容哈希（客户端计算，为空时服务端兜底计算）
     * @return 更新后的争议案件视图对象
     */
    DisputeCaseVO addEvidence(Long submitterAccountId, String caseNo,
                              String evidenceType, String fileUrl, String description, String fileHash);

    /**
     * 处理争议案件
     * <p>
     * 完整状态机流转：
     * <ul>
     *   <li>ACCEPT: DISPUTE_SUBMITTED → DISPUTE_ACCEPTED</li>
     *   <li>EVIDENCE_PENDING: DISPUTE_ACCEPTED → DISPUTE_EVIDENCE_PENDING</li>
     *   <li>REVIEW: DISPUTE_ACCEPTED/DISPUTE_EVIDENCE_PENDING → DISPUTE_REVIEWING</li>
     *   <li>RESOLVE: 任意非关闭状态 → DISPUTE_RESOLVED（结论上链）</li>
     *   <li>REJECT: 任意非关闭状态 → DISPUTE_REJECTED（结论上链）</li>
     *   <li>CLOSE: 任意非关闭状态 → DISPUTE_CLOSED（结论上链）</li>
     * </ul>
     * 每次流转记录处理记录和状态变更历史。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    处理请求
     * @return 更新后的争议案件视图对象
     */
    DisputeCaseVO processDispute(Long operatorId, DisputeProcessRequest request);

    /**
     * 查询争议案件详情
     * <p>
     * 包含案件基本信息、全部证据列表和处理记录列表。
     * 仅案件相关方（申请人/被申请人）或管理员/监管员可查看。
     * </p>
     *
     * @param caseNo          案件编号
     * @param viewerAccountId 查看者账户ID（用于归属校验）
     * @return 争议案件视图对象（含证据和处理记录）
     */
    DisputeCaseVO getDisputeDetail(String caseNo, Long viewerAccountId);

    /**
     * 分页查询争议案件列表
     *
     * @param accountId 关联账户ID（可选，查询与该账户相关的争议）
     * @param status    争议状态（可选）
     * @param query     分页参数
     * @return 分页争议案件列表
     */
    PageResult<DisputeCaseVO> listDisputes(Long accountId, String status, PageQuery query);

    /**
     * 监管员分页查询全量争议案件列表
     * <p>
     * 不按申请人/被申请人归属过滤，面向监管视角按条件分页查询全部案件。
     * 列表不加载完整证据和处理记录，避免过重。
     * </p>
     *
     * @param regulatorQuery 监管员筛选条件
     * @param pageQuery      分页参数
     * @return 分页争议案件列表
     */
    PageResult<RegulatorDisputeListVO> listRegulatorDisputes(RegulatorDisputeQuery regulatorQuery, PageQuery pageQuery);
}
