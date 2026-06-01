package com.lifechain.work.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.work.dto.ClaimDetailVO;
import com.lifechain.work.dto.ClaimListVO;
import com.lifechain.work.dto.ClaimReviewRequest;
import com.lifechain.work.dto.ClaimSubmitRequest;

/**
 * 确权服务接口
 * <p>
 * 提供确权申请提交、详情查询、审核及确权列表查询等核心功能。
 * 确权流程：提交 -> 审核 -> 通过后上链 -> 链上确认 -> 确权成功。
 * </p>
 *
 * @author LifeChain
 */
public interface ClaimService {

    /**
     * 提交确权申请
     * <p>
     * 验证DID已生效、作品处于READY_FOR_CLAIM状态、无重复有效确权申请，
     * 创建确权申请记录，更新作品状态为CLAIM_REVIEWING。
     * </p>
     *
     * @param accountId 申请人账户ID
     * @param request   确权申请请求
     * @return 确权详情
     */
    ClaimDetailVO submitClaim(Long accountId, ClaimSubmitRequest request);

    /**
     * 查询确权详情
     *
     * @param claimNo         确权编号
     * @param viewerAccountId 查看者账户ID（用于归属校验）
     * @return 确权详情
     */
    ClaimDetailVO getClaimDetail(String claimNo, Long viewerAccountId);

    /**
     * 查询确权链上回执信息
     * <p>
     * 允许确权申请人查看链上回执（chainInfo），非申请人/管理员/监管员无权查看。
     * </p>
     *
     * @param claimNo         确权编号
     * @param viewerAccountId 查看者账户ID
     * @return 确权详情（含chainInfo）
     */
    ClaimDetailVO getClaimChainReceipt(String claimNo, Long viewerAccountId);

    /**
     * 审核确权申请
     * <p>
     * 审核通过时：设置为CLAIM_APPROVED_PENDING_CHAIN，调用链码上链，
     * 上链成功后设置为CLAIM_SUCCESS。
     * 审核驳回时：设置为CLAIM_REJECTED，作品回退到READY_FOR_CLAIM。
     * </p>
     *
     * @param reviewerId 审核人ID
     * @param request    审核请求
     */
    void reviewClaim(Long reviewerId, ClaimReviewRequest request);

    /**
     * 查询确权申请列表（分页）
     *
     * @param accountId 申请人账户ID
     * @param status    状态筛选（可为null）
     * @param query     分页参数
     * @return 分页结果
     */
    PageResult<ClaimListVO> listClaims(Long accountId, String status, PageQuery query);

    /**
     * 管理员查询全部确权申请列表（分页）
     *
     * @param status 状态筛选（可为null）
     * @param query  分页参数
     * @return 分页结果
     */
    PageResult<ClaimListVO> listAllClaims(String status, PageQuery query);
}
