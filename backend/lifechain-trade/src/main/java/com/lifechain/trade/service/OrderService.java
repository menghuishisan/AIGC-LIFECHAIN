package com.lifechain.trade.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.infra.payment.PaymentCallbackResult;
import com.lifechain.trade.dto.*;

/**
 * 订单服务接口
 * <p>
 * 提供交易订单的完整生命周期管理，包括创建订单、发起支付、处理支付回调、
 * 取消订单、申请退款和处理退款等功能。
 * 订单状态机严格管控每一步流转，确保交易安全和数据一致性。
 * </p>
 *
 * @author LifeChain
 */
public interface OrderService {

    /**
     * 创建交易订单
     * <p>
     * 校验上架状态、买方身份（不能购买自己的作品）、幂等性，
     * 创建订单并锁定快照信息（作品、上架、分账规则），设置30分钟过期时间。
     * </p>
     *
     * @param buyerAccountId 买方账户ID
     * @param request        创建订单请求
     * @return 订单详情
     */
    OrderDetailVO createOrder(Long buyerAccountId, CreateOrderRequest request);

    /**
     * 查询订单详情
     *
     * @param orderNo         订单编号
     * @param viewerAccountId 查看者账户ID
     * @return 订单详情
     */
    OrderDetailVO getOrderDetail(String orderNo, Long viewerAccountId);

    /**
     * 分页查询我的订单列表
     *
     * @param accountId 当前用户账户ID
     * @param role      角色（BUYER-买方/CREATOR-创作者）
     * @param status    订单状态（可选）
     * @param query     分页参数
     * @return 分页订单列表
     */
    PageResult<OrderListVO> listMyOrders(Long accountId, String role, String status, PageQuery query);

    /**
     * 发起支付
     * <p>
     * 校验订单状态和过期时间，调用支付适配器创建支付单，
     * 创建支付记录并更新订单状态为 PAY_PENDING_CONFIRM。
     * </p>
     *
     * @param buyerAccountId 买方账户ID
     * @param request        支付请求
     * @return 支付结果（含前端调起支付所需参数）
     */
    PayResultVO payOrder(Long buyerAccountId, PayRequest request);

    /**
     * 处理支付回调
     * <p>
     * 核心支付回调处理逻辑，必须保证幂等性。
     * 验证金额一致性，更新支付记录和订单状态，触发授权生成。
     * </p>
     *
     * @param payChannel     支付渠道
     * @param callbackResult 回调解析结果
     */
    void handlePaymentCallback(String payChannel, PaymentCallbackResult callbackResult);

    /**
     * 取消订单
     * <p>
     * 校验订单状态为 ORDER_CREATED 或 PAY_PENDING_CONFIRM，
     * 关闭关联支付记录，更新订单状态为 ORDER_CANCELLED。
     * </p>
     *
     * @param buyerAccountId 买方账户ID
     * @param orderNo        订单编号
     */
    void cancelOrder(Long buyerAccountId, String orderNo);

    /**
     * 申请退款
     * <p>
     * 校验订单状态为 AUTH_GRANTED 或 ORDER_COMPLETED，
     * 创建退款记录并更新订单状态为 REFUND_PENDING。
     * </p>
     *
     * @param buyerAccountId 买方账户ID
     * @param request        退款申请请求
     */
    void applyRefund(Long buyerAccountId, RefundApplyRequest request);

    /**
     * 处理退款审批
     * <p>
     * 管理员审批退款申请，通过后调用支付渠道退款接口，
     * 更新退款记录和订单状态，触发逆分账。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    退款处理请求
     */
    void processRefund(Long operatorId, RefundProcessRequest request);

    /**
     * 分页查询退款列表（管理员）
     *
     * @param status 退款状态（可选）
     * @param query  分页参数
     * @return 分页退款列表
     */
    PageResult<RefundDetailVO> listRefunds(String status, PageQuery query);

    /**
     * 查询退款详情（管理员）
     *
     * @param refundNo 退款编号
     * @return 退款详情
     */
    RefundDetailVO getRefundDetail(String refundNo);

    /**
     * 管理员分页查询全量订单列表
     * <p>
     * 不按当前登录用户归属过滤，按管理员筛选条件分页查询所有订单。
     * </p>
     *
     * @param adminQuery 管理员筛选条件
     * @param pageQuery  分页参数
     * @return 分页订单列表
     */
    PageResult<AdminOrderListVO> listAdminOrders(AdminOrderQuery adminQuery, PageQuery pageQuery);
}
