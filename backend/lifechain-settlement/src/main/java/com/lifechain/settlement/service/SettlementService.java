package com.lifechain.settlement.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.settlement.dto.ReverseSettlementVO;
import com.lifechain.settlement.dto.SettlementRecordVO;

/**
 * 结算服务接口
 * <p>
 * 核心分账结算服务，处理订单的分账执行、结算查询、失败重试和逆分账等操作。
 * 所有结算信息同步上链存证，确保资金分配的全链路可追溯。
 * </p>
 *
 * @author LifeChain
 */
public interface SettlementService {

    /**
     * 执行订单分账结算
     * <p>
     * 根据作品绑定的结算规则计算各角色的分账金额，创建结算记录并提交链上存证。
     * 若作品无绑定规则，则使用默认80/20比例（创作者80%，平台20%）。
     * </p>
     *
     * @param orderNo          订单编号
     * @param orderId          订单ID
     * @param workId           作品ID
     * @param workNo           作品编号
     * @param totalAmount      结算总金额（单位：分）
     * @param creatorAccountId 创作者账户ID
     * @return 结算记录视图对象
     */
    SettlementRecordVO settleOrder(String orderNo, Long orderId, Long workId,
                                    String workNo, Long totalAmount, Long creatorAccountId);

    /**
     * 根据订单编号查询结算记录
     *
     * @param orderNo 订单编号
     * @return 结算记录视图对象
     */
    SettlementRecordVO getSettlementByOrderNo(String orderNo);

    /**
     * 根据结算编号查询结算记录
     *
     * @param settleNo 结算编号
     * @return 结算记录视图对象
     */
    SettlementRecordVO getSettlementBySettleNo(String settleNo);

    /**
     * 分页查询结算记录列表（管理员）
     *
     * @param status 结算状态（可选）
     * @param query  分页参数
     * @return 分页结算列表
     */
    PageResult<SettlementRecordVO> listSettlements(String status, PageQuery query);

    /**
     * 重试失败的结算
     * <p>
     * 仅允许对状态为 SETTLE_FAILED 的结算记录进行重试，
     * 重新提交链上存证请求。
     * </p>
     *
     * @param settleNo 结算编号
     * @return 更新后的结算记录视图对象
     */
    SettlementRecordVO retrySettlement(String settleNo);

    /**
     * 发起逆分账
     * <p>
     * 针对已成功结算的订单发起逆分账操作，将已分账资金按原路退回。
     * 仅允许对状态为 SETTLE_SUCCESS 的结算发起逆分账。
     * </p>
     *
     * @param settleNo 原结算编号
     * @param reason   逆分账原因
     * @return 逆分账记录视图对象
     */
    ReverseSettlementVO reverseSettlement(String settleNo, String reason);
}
