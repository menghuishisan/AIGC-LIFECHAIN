package com.lifechain.auth.service;

import com.lifechain.auth.dto.DidApplyRequest;
import com.lifechain.auth.dto.DidInfoVO;
import com.lifechain.auth.dto.DidReviewRequest;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;

/**
 * DID（数字身份）服务接口
 * <p>
 * 提供DID的申请、审核、查询、挂起、吊销等完整生命周期管理功能。
 * DID审核通过后将自动触发Fabric链上注册操作。
 * </p>
 *
 * @author LifeChain
 */
public interface DidService {

    /**
     * 申请DID
     *
     * @param accountId 账户ID
     * @param request   申请请求
     */
    void applyDid(Long accountId, DidApplyRequest request);

    /**
     * 审核DID（管理员）
     *
     * @param reviewerId 审核人ID
     * @param request    审核请求
     */
    void reviewDid(Long reviewerId, DidReviewRequest request);

    /**
     * 查询DID信息（用户侧，仅允许查看自己的DID）
     *
     * @param didNo     DID编号
     * @param accountId 当前登录账户ID
     * @return DID信息视图对象
     */
    DidInfoVO getDidInfo(String didNo, Long accountId);

    /**
     * 分页查询DID列表（管理员）
     *
     * @param status    DID状态（可选）
     * @param accountNo 账户编号（可选）
     * @param query     分页参数
     * @return 分页DID列表
     */
    PageResult<DidInfoVO> listDids(String status, String accountNo, PageQuery query);

    /**
     * 挂起DID（管理员）
     *
     * @param operatorId 操作人ID
     * @param didNo      DID编号
     * @param reason     挂起原因
     */
    void suspendDid(Long operatorId, String didNo, String reason);

    /**
     * 吊销DID（管理员）
     *
     * @param operatorId 操作人ID
     * @param didNo      DID编号
     * @param reason     吊销原因
     */
    void revokeDid(Long operatorId, String didNo, String reason);
}
