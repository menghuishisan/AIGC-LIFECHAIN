package com.lifechain.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.auth.audit.AuditService;
import com.lifechain.auth.audit.TraceEventService;
import com.lifechain.auth.entity.AccountEntity;
import com.lifechain.auth.entity.DidRecordEntity;
import com.lifechain.auth.mapper.AccountMapper;
import com.lifechain.auth.mapper.DidRecordMapper;
import com.lifechain.chain.adapter.LicenseChainAdapter;
import com.lifechain.chain.model.ChainSubmitResult;
import com.lifechain.chain.record.ChainTxRecordService;
import com.lifechain.common.enums.*;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.mq.OrderExpireMessage;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.common.util.MoneyUtil;
import com.lifechain.infra.notification.NotificationService;
import com.lifechain.infra.mq.MessagePublisher;
import com.lifechain.infra.mq.RabbitMQConfig;
import com.lifechain.infra.payment.*;
import com.lifechain.infra.redis.RedisService;
import com.lifechain.settlement.entity.WorkSettleRuleEntity;
import com.lifechain.settlement.mapper.SettlementRecordMapper;
import com.lifechain.settlement.mapper.WorkSettleRuleMapper;
import com.lifechain.settlement.service.SettlementService;
import com.lifechain.trade.assembler.TradeVoAssembler;
import com.lifechain.trade.dto.*;
import com.lifechain.trade.entity.*;
import com.lifechain.trade.mapper.*;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.WorkMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 订单服务实现
 * <p>
 * 实现交易订单的完整生命周期管理，包括创建订单、发起支付、处理支付回调、
 * 自动授权上链、取消订单、申请退款和处理退款等核心业务逻辑。
 * 所有状态变更严格遵循状态机规范，并记录审计日志和状态变更历史。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final TradeOrderMapper orderMapper;
    private final TradeOrderSnapshotMapper snapshotMapper;
    private final AccountMapper accountMapper;
    private final DidRecordMapper didRecordMapper;
    private final PaymentRecordMapper paymentMapper;
    private final PaymentNotificationMapper paymentNotificationMapper;
    private final LicenseRecordMapper licenseMapper;
    private final RefundRecordMapper refundMapper;
    private final WorkListingMapper listingMapper;
    private final WorkMapper workMapper;
    private final PaymentAdapterFactory paymentAdapterFactory;
    private final LicenseChainAdapter licenseChainAdapter;
    private final ChainTxRecordService chainTxRecordService;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final ObjectMapper objectMapper;
    private final WorkSettleRuleMapper workSettleRuleMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final SettlementService settlementService;
    private final NotificationService notificationService;
    private final RedisService redisService;
    private final MessagePublisher messagePublisher;
    private final PlatformTransactionManager transactionManager;

    @Value("${lifechain.pay.wechat.notifyUrl:}")
    private String wechatNotifyUrl;

    @Value("${lifechain.pay.alipay.notifyUrl:}")
    private String alipayNotifyUrl;

    /**
     * {@inheritDoc}
     * <p>
     * 业务规则：
     * <ol>
     *   <li>上架记录必须存在且状态为 LISTED</li>
     *   <li>不能购买自己的作品</li>
     *   <li>创建订单并保存上架信息快照</li>
     *   <li>订单过期时间为30分钟</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO createOrder(Long buyerAccountId, CreateOrderRequest request) {
        log.info("创建订单，buyerAccountId={}, listingNo={}", buyerAccountId, request.getListingNo());

        // 0. 分布式锁：防止同一上架记录被并发下单
        String lockKey = "lock:order:listing:" + request.getListingNo();
        boolean locked = redisService.setIfAbsent(lockKey, String.valueOf(buyerAccountId), 10, java.util.concurrent.TimeUnit.SECONDS);
        if (!locked) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "订单正在处理中，请勿重复提交");
        }
        try {
            return doCreateOrder(buyerAccountId, request);
        } finally {
            redisService.delete(lockKey);
        }
    }

    private OrderDetailVO doCreateOrder(Long buyerAccountId, CreateOrderRequest request) {
        // 1. 查询上架信息
        WorkListingEntity listing = listingMapper.selectByListingNo(request.getListingNo());
        if (listing == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "上架记录不存在");
        }
        if (!"LISTED".equals(listing.getStatus())) {
            throw new BizException(ErrorCodeEnum.WORK_NOT_LISTED, "作品未上架或已下架");
        }

        // 2. 不能购买自己的作品
        if (buyerAccountId.equals(listing.getCreatorAccountId())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "不能购买自己的作品");
        }

        // 2.5 交易前校验授权双方都具备有效DID，确保后续授权上链主体归属清晰
        DidRecordEntity creatorDid = assertActiveDidForTrade(listing.getCreatorAccountId(), "创作者");
        DidRecordEntity buyerDid = assertActiveDidForTrade(buyerAccountId, "购买方");

        // 3. 创建订单
        MoneyUtil.requireFen(listing.getPriceAmount());
        MoneyUtil.requireCny(listing.getCurrency());
        PayChannelEnum.fromCode(request.getPayChannel());
        if (PayChannelEnum.WECHAT_PAY.getCode().equals(request.getPayChannel())
                && listing.getPriceAmount() > Integer.MAX_VALUE) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "金额超出微信支付渠道范围，请选择支付宝");
        }
        WorkSettleRuleEntity fixedRule = workSettleRuleMapper.selectEffectiveRuleByWorkNo(listing.getWorkNo());
        var creatorRatio = fixedRule == null ? new java.math.BigDecimal("0.8000") : fixedRule.getCreatorRatio();
        var platformRatio = fixedRule == null ? new java.math.BigDecimal("0.2000") : fixedRule.getPlatformRatio();
        MoneyUtil.requireRatios(creatorRatio, platformRatio);
        LocalDateTime now = DateTimeUtil.nowUtc();
        TradeOrderEntity order = new TradeOrderEntity();
        order.setOrderNo(BizNoUtil.orderNo());
        order.setWorkId(listing.getWorkId());
        order.setWorkNo(listing.getWorkNo());
        order.setListingId(listing.getId());
        order.setListingNo(listing.getListingNo());
        order.setBuyerAccountId(buyerAccountId);
        order.setBuyerSubjectId(buyerDid.getSubjectId());
        order.setCreatorAccountId(listing.getCreatorAccountId());
        order.setCreatorSubjectId(creatorDid.getSubjectId());
        order.setOrderStatus(OrderStatusEnum.ORDER_CREATED.getCode());
        order.setLicenseType(listing.getLicenseType());
        order.setPriceAmount(listing.getPriceAmount());
        order.setPayAmount(listing.getPriceAmount());
        order.setCreatorRatio(creatorRatio);
        order.setPlatformRatio(platformRatio);
        order.setCurrency(listing.getCurrency());
        order.setPayChannel(request.getPayChannel());
        order.setPayStatus(PayStatusEnum.PAY_INIT.getCode());
        order.setExpireTime(now.plusMinutes(30));
        order.setRequestId(request.getRequestId());
        orderMapper.insert(order);

        // 4. 创建订单快照（上架信息快照）
        try {
            String listingJson = objectMapper.writeValueAsString(listing);
            TradeOrderSnapshotEntity snapshot = new TradeOrderSnapshotEntity();
            snapshot.setOrderId(order.getId());
            snapshot.setOrderNo(order.getOrderNo());
            snapshot.setSnapshotType("LISTING_INFO");
            snapshot.setSnapshotData(listingJson);
            snapshot.setSnapshotHash(HashUtil.sha256(listingJson));
            snapshot.setSnapshotTime(now);
            snapshotMapper.insert(snapshot);
        } catch (Exception e) {
            log.warn("创建订单快照失败: orderNo={}", order.getOrderNo(), e);
        }

        // 4.1 创建分账规则快照
        try {
            WorkSettleRuleEntity settleRule = fixedRule;
            if (settleRule != null) {
                String ruleJson = objectMapper.writeValueAsString(settleRule);
                TradeOrderSnapshotEntity ruleSnapshot = new TradeOrderSnapshotEntity();
                ruleSnapshot.setOrderId(order.getId());
                ruleSnapshot.setOrderNo(order.getOrderNo());
                ruleSnapshot.setSnapshotType("SETTLE_RULE");
                ruleSnapshot.setSnapshotData(ruleJson);
                ruleSnapshot.setSnapshotHash(HashUtil.sha256(ruleJson));
                ruleSnapshot.setSnapshotTime(now);
                snapshotMapper.insert(ruleSnapshot);
            }
        } catch (Exception e) {
            log.warn("创建分账规则快照失败: orderNo={}", order.getOrderNo(), e);
        }

        // 5. 写入状态历史
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                null, OrderStatusEnum.ORDER_CREATED.getCode(),
                "创建订单", null, buyerAccountId);

        traceEventService.writeTraceEvent(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                "ORDER_CREATED", "订单创建成功", buyerAccountId, null, null);

        messagePublisher.sendDelayed(RabbitMQConfig.RK_ORDER_EXPIRE_DELAY,
                new OrderExpireMessage(order.getId(), order.getOrderNo()),
                30 * 60 * 1000L);

        log.info("订单创建成功: orderNo={}, workNo={}, buyerAccountId={}",
                order.getOrderNo(), order.getWorkNo(), buyerAccountId);
        return buildOrderDetailVO(order);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 业务规则：
     * <ol>
     *   <li>只有买方本人可发起支付</li>
     *   <li>订单必须处于 ORDER_CREATED 状态</li>
     *   <li>订单未过期</li>
     *   <li>调用第三方支付渠道创建支付单</li>
     *   <li>创建支付记录、更新订单状态为 PAY_PENDING_CONFIRM</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResultVO payOrder(Long buyerAccountId, String orderNo, PayRequest request) {
        log.info("发起支付，buyerAccountId={}, orderNo={}", buyerAccountId, orderNo);

        // 1. 查询订单
        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "非本人订单");
        }

        if (!order.getPayChannel().equals(request.getPayChannel())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "支付渠道必须与订单一致");
        }

        // 2. 校验订单状态
        boolean resume = PayChannelEnum.ALIPAY.getCode().equals(order.getPayChannel())
                && order.getPayChannel().equals(request.getPayChannel())
                && OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(order.getOrderStatus());
        if (!resume && !OrderStatusEnum.ORDER_CREATED.getCode().equals(order.getOrderStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "订单状态不允许支付",
                    null, order.getOrderStatus());
        }

        // 3. 校验是否过期
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(DateTimeUtil.nowUtc())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "订单已过期");
        }

        // 3.5 支付前再次校验授权双方DID，避免创建订单后身份状态变化导致支付成功却无法授权上链
        assertActiveDidForTrade(order.getCreatorAccountId(), "创作者");
        assertActiveDidForTrade(order.getBuyerAccountId(), "购买方");

        // 4. 确定通知地址
        String notifyUrl = resolveNotifyUrl(request.getPayChannel());

        // 5. 调用支付适配层创建支付单
        PaymentRequest payReq = new PaymentRequest();
        payReq.setOrderNo(order.getOrderNo());
        payReq.setSubject("AIGC作品授权-" + order.getWorkNo());
        payReq.setTotalAmount(order.getPayAmount());
        payReq.setExpireTime(order.getExpireTime());
        payReq.setPayChannel(request.getPayChannel());
        payReq.setNotifyUrl(notifyUrl);
        payReq.setClientIp(request.getClientIp());
        payReq.setRequestId(request.getRequestId());

        PaymentResponse payResp = paymentAdapterFactory.createPayment(payReq);
        if (!payResp.isSuccess()) {
            throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, payResp.getErrorMsg());
        }

        // 已有支付会话复用同一商户订单号和支付记录，浏览器刷新不重复记账。
        if (resume) {
            PayResultVO result = new PayResultVO();
            result.setOrderNo(order.getOrderNo());
            result.setPayParams(payResp.getPayParams());
            result.setPayUrl(payResp.getPayUrl());
            result.setFormHtml(payResp.getFormHtml());
            result.setPrepayId(payResp.getPrepayId());
            return result;
        }

        // 6. 创建支付记录
        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setPaymentNo(BizNoUtil.paymentNo());
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPayChannel(request.getPayChannel());
        payment.setPayStatus(PayStatusEnum.PAY_PENDING.getCode());
        payment.setPayAmount(order.getPayAmount());
        payment.setCurrency(order.getCurrency());
        payment.setPrepayId(payResp.getPrepayId());
        payment.setExpireTime(order.getExpireTime());
        payment.setRequestId(request.getRequestId());
        paymentMapper.insert(payment);

        // 7. 更新订单状态
        order.setOrderStatus(OrderStatusEnum.PAY_PENDING_CONFIRM.getCode());
        order.setPayChannel(request.getPayChannel());
        order.setPayStatus(PayStatusEnum.PAY_PENDING.getCode());
        orderMapper.updateById(order);

        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                OrderStatusEnum.ORDER_CREATED.getCode(), OrderStatusEnum.PAY_PENDING_CONFIRM.getCode(),
                "发起支付", null, buyerAccountId);

        traceEventService.writeTraceEvent(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                "ORDER_PAY_INITIATED", "支付发起成功", buyerAccountId, null, null);

        log.info("支付发起成功: orderNo={}, paymentNo={}", order.getOrderNo(), payment.getPaymentNo());

        // 8. 返回支付参数
        PayResultVO result = new PayResultVO();
        result.setOrderNo(order.getOrderNo());
        result.setPrepayId(payResp.getPrepayId());
        result.setPayParams(payResp.getPayParams());
        result.setPayUrl(payResp.getPayUrl());
        result.setFormHtml(payResp.getFormHtml());
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 核心支付回调处理逻辑，保证严格幂等：
     * <ol>
     *   <li>订单不存在直接忽略</li>
     *   <li>若订单已处于支付确认之后的状态，幂等返回</li>
     *   <li>校验支付金额一致性，不一致则标记异常</li>
     *   <li>更新支付记录和订单状态</li>
     *   <li>支付成功后自动触发授权上链</li>
     * </ol>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(String payChannel, PaymentCallbackResult result) {
        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(result.getOrderNo());
        if (order == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 业务归属、金额和流水校验先于幂等返回，重复通知也不能绕过校验。
        if (!payChannel.equals(order.getPayChannel()) || !payChannel.equals(result.getPayChannel())) {
            throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "支付渠道不匹配");
        }
        if ((result.isSuccess() || result.isNonPaymentEvent())
                && !java.util.Objects.equals(order.getPayAmount(), result.getPaidAmount())) {
            throw new BizException(ErrorCodeEnum.PAY_AMOUNT_MISMATCH);
        }
        List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
        PaymentRecordEntity payment = payments.stream().filter(p -> payChannel.equals(p.getPayChannel()))
                .findFirst().orElseThrow(() -> new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "缺少支付记录"));
        if (result.isSuccess() && (result.getThirdTradeNo() == null || result.getThirdTradeNo().isBlank())) {
            throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "缺少渠道交易流水");
        }
        if (payment.getThirdTradeNo() != null && !payment.getThirdTradeNo().equals(result.getThirdTradeNo())) {
            throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "支付流水不匹配");
        }
        if (result.getNotifyId() != null) {
            var previous = paymentNotificationMapper.selectByNotifyId(payChannel, result.getNotifyId());
            String payloadHash = HashUtil.sha256(result.getRawPayload());
            if (previous != null) {
                if (!order.getOrderNo().equals(previous.getOrderNo()) || !payloadHash.equals(previous.getPayloadHash())) {
                    throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "通知标识与既有记录不匹配");
                }
                return;
            }
            var notification = new PaymentNotificationEntity();
            notification.setPayChannel(payChannel);
            notification.setNotifyId(result.getNotifyId());
            notification.setOrderNo(order.getOrderNo());
            notification.setThirdTradeNo(result.getThirdTradeNo());
            notification.setTradeStatus(result.getTradeStatus());
            notification.setPayloadHash(payloadHash);
            paymentNotificationMapper.insert(notification);
        }
        // 退款、关闭、分账及等待付款通知不修改付款状态，尤其不能将已付款订单降级。
        if (result.isNonPaymentEvent() || order.getPayTime() != null
                || PayStatusEnum.PAY_SUCCESS.getCode().equals(payment.getPayStatus())) {
            return;
        }
        LocalDateTime now = DateTimeUtil.nowUtc();
        payment.setThirdTradeNo(result.getThirdTradeNo());
        payment.setPayTime(result.getPayTime());
        payment.setCallbackTime(now);
        payment.setCallbackRawRef(result.getRawPayload() == null ? null : HashUtil.sha256(result.getRawPayload()));
        payment.setPayStatus(result.isSuccess() ? PayStatusEnum.PAY_SUCCESS.getCode() : PayStatusEnum.PAY_FAILED.getCode());
        paymentMapper.updateById(payment);
        if (!result.isSuccess()) return;

        String fromStatus = order.getOrderStatus();
        order.setPayStatus(PayStatusEnum.PAY_SUCCESS.getCode());
        order.setPayTime(result.getPayTime() == null ? now : result.getPayTime());
        // 失效订单发生晚到付款时保留资金事实并转异常处理，禁止继续交付授权。
        if (!OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(fromStatus)) {
            order.setOrderStatus(OrderStatusEnum.ORDER_EXCEPTION.getCode());
            orderMapper.updateById(order);
            auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                    fromStatus, order.getOrderStatus(), "失效订单收到付款，需人工核实退款", "LATE_PAYMENT", null);
            return;
        }
        order.setOrderStatus(OrderStatusEnum.PAY_CONFIRMED.getCode());
        orderMapper.updateById(order);
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                fromStatus, OrderStatusEnum.PAY_CONFIRMED.getCode(), "支付确认", null, null);
        auditService.writeAuditLog(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                "PAY_CONFIRMED", "支付确认,第三方流水号:" + result.getThirdTradeNo(),
                null, null, null, "SUCCESS", null);
        traceEventService.writeTraceEvent(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                "ORDER_PAY_CONFIRMED", "支付确认成功", null, null, null);
        notificationService.sendNotice(order.getBuyerAccountId(), "支付成功",
                "您的订单 " + order.getOrderNo() + " 已支付成功，授权正在处理中。", "ORDER", "ORDER", order.getOrderNo());
        notificationService.sendNotice(order.getCreatorAccountId(), "作品被购买",
                "您的作品已被购买，订单号：" + order.getOrderNo() + "，授权正在上链处理。", "ORDER", "ORDER", order.getOrderNo());
        grantLicense(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailVO refreshPayment(Long buyerAccountId, String orderNo) {
        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null) throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED);
        }
        if (PayChannelEnum.ALIPAY.getCode().equals(order.getPayChannel())
                && OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(order.getOrderStatus())) {
            PaymentQueryResult query = paymentAdapterFactory.getQueryableAdapter(order.getPayChannel()).queryPayment(orderNo);
            if (query.isPaid()) {
                handlePaymentCallback(order.getPayChannel(), PaymentCallbackResult.builder().orderNo(orderNo)
                        .thirdTradeNo(query.getThirdTradeNo()).paidAmount(query.getTotalAmount())
                        .payChannel(order.getPayChannel()).payTime(query.getPayTime()).success(true)
                        .tradeStatus(query.getTradeStatus()).build());
                order = orderMapper.selectByOrderNo(orderNo);
            }
        }
        return buildOrderDetailVO(order);
    }

    /** 延迟队列、交易补偿共用同一过期事务；未知渠道结果不关闭本地订单。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireOrder(String orderNo) {
        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || order.getExpireTime() == null
                || order.getExpireTime().isAfter(DateTimeUtil.nowUtc())) return;
        String status = order.getOrderStatus();
        if (OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(status)) {
            if (!PayChannelEnum.ALIPAY.getCode().equals(order.getPayChannel())) return;
            var adapter = paymentAdapterFactory.getQueryableAdapter(order.getPayChannel());
            PaymentQueryResult query = adapter.queryPayment(orderNo);
            if (query.isPaid()) {
                handlePaymentCallback(order.getPayChannel(), PaymentCallbackResult.builder().orderNo(orderNo)
                        .thirdTradeNo(query.getThirdTradeNo()).paidAmount(query.getTotalAmount())
                        .payChannel(order.getPayChannel()).payTime(query.getPayTime()).success(true)
                        .tradeStatus(query.getTradeStatus()).build());
                return;
            }
            if (!Set.of("WAIT_BUYER_PAY", "TRADE_CLOSED", "NOT_FOUND").contains(query.getTradeStatus())) {
                throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "付款结果未知，稍后重试过期处理");
            }
            adapter.closePayment(orderNo);
        } else if (!OrderStatusEnum.ORDER_CREATED.getCode().equals(status)) {
            return;
        }
        order.setOrderStatus(OrderStatusEnum.ORDER_EXPIRED.getCode());
        order.setPayStatus(PayStatusEnum.PAY_CLOSED.getCode());
        order.setCancelTime(DateTimeUtil.nowUtc());
        order.setCancelReason("订单超时自动过期");
        orderMapper.updateById(order);
        for (PaymentRecordEntity payment : paymentMapper.selectByOrderId(order.getId())) {
            if (Set.of(PayStatusEnum.PAY_INIT.getCode(), PayStatusEnum.PAY_PENDING.getCode()).contains(payment.getPayStatus())) {
                payment.setPayStatus(PayStatusEnum.PAY_CLOSED.getCode());
                paymentMapper.updateById(payment);
            }
        }
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                status, order.getOrderStatus(), "订单超时自动过期", "ORDER_EXPIRED", null);
    }

    /**
     * 授权生效（内部方法）
     * <p>
     * 支付确认后自动触发：创建授权记录、调用链码登记授权，
     * 成功后依次推进订单状态到 AUTH_GRANTED → SETTLEMENT_PENDING。
     * 链上失败时授权记录标记 CHAIN_FAILED，待后续补偿任务重试。
     * </p>
     *
     * @param order 已支付确认的订单实体
     */
    private void grantLicense(TradeOrderEntity order) {
        LocalDateTime now = DateTimeUtil.nowUtc();

        // 1. 查询上架信息获取授权范围
        WorkListingEntity listing = listingMapper.selectByListingNo(order.getListingNo());

        // 2. 创建授权记录
        LicenseRecordEntity license = new LicenseRecordEntity();
        license.setLicenseNo(BizNoUtil.licenseNo());
        license.setOrderId(order.getId());
        license.setOrderNo(order.getOrderNo());
        license.setWorkId(order.getWorkId());
        license.setWorkNo(order.getWorkNo());
        license.setLicensorAccountId(order.getCreatorAccountId());
        license.setLicenseeAccountId(order.getBuyerAccountId());
        license.setLicenseType(order.getLicenseType());
        license.setLicenseStatus(LicenseStatusEnum.LICENSE_PENDING.getCode());
        license.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
        if (listing != null) {
            license.setScopeDescription(listing.getScopeDescription());
            if (listing.getDurationDays() != null && listing.getDurationDays() > 0) {
                license.setExpireTime(now.plusDays(listing.getDurationDays()));
            }
        }
        license.setEffectiveTime(now);
        String licenseData = license.getLicenseNo() + license.getWorkNo()
                + license.getLicenseType() + now;
        license.setLicenseHash(HashUtil.sha256(licenseData));
        licenseMapper.insert(license);

        // 3. 更新订单到 AUTH_GRANTING
        order.setOrderStatus(OrderStatusEnum.AUTH_GRANTING.getCode());
        orderMapper.updateById(order);
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                OrderStatusEnum.PAY_CONFIRMED.getCode(), OrderStatusEnum.AUTH_GRANTING.getCode(),
                "授权处理中", null, null);

        submitLicense(order, license, now);
    }

    /**
     * 背书失败没有提交交易，可在依赖恢复后重试；已提交但结果未知的交易由链回执任务查询。
     * 订单行锁与退款共用，复用原授权记录，避免重复交付或退款后重新激活。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryLicenseGrant(String orderNo) {
        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || !PayStatusEnum.PAY_SUCCESS.getCode().equals(order.getPayStatus())
                || !OrderStatusEnum.ORDER_EXCEPTION.getCode().equals(order.getOrderStatus())) return;
        LicenseRecordEntity license = licenseMapper.selectByOrderId(order.getId());
        if (license == null || !LicenseStatusEnum.LICENSE_PENDING.getCode().equals(license.getLicenseStatus())
                || !ChainStatusEnum.CHAIN_FAILED.getCode().equals(license.getChainStatus())
                || (license.getTxHash() != null && !license.getTxHash().isBlank())) return;
        var attempt = chainTxRecordService.getByBizTypeAndBizNo(BizTypeEnum.LICENSE.getCode(), license.getLicenseNo());
        if (attempt == null || !"REGISTER".equals(attempt.getTxType())
                || !ChainStatusEnum.CHAIN_FAILED.getCode().equals(attempt.getChainStatus())
                || (attempt.getTxHash() != null && !attempt.getTxHash().isBlank())
                || !ErrorCodeEnum.CHAIN_SUBMIT_FAILED.getCode().equals(attempt.getReasonCode())
                || attempt.getFailReason() == null || !attempt.getFailReason().startsWith("链码背书失败:")) return;
        DidRecordEntity creator = assertActiveDidForTrade(order.getCreatorAccountId(), "创作者");
        DidRecordEntity buyer = assertActiveDidForTrade(order.getBuyerAccountId(), "购买方");
        if (!creator.getSubjectId().equals(order.getCreatorSubjectId())
                || !buyer.getSubjectId().equals(order.getBuyerSubjectId())) return;
        order.setOrderStatus(OrderStatusEnum.AUTH_GRANTING.getCode());
        orderMapper.updateById(order);
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                OrderStatusEnum.ORDER_EXCEPTION.getCode(), OrderStatusEnum.AUTH_GRANTING.getCode(),
                "重试未提交的授权背书，复用原授权记录", null, null);
        submitLicense(order, license, license.getEffectiveTime());
    }

    private void submitLicense(TradeOrderEntity order, LicenseRecordEntity license, LocalDateTime effectiveTime) {
        // 查询授权方与被授权方的真实DID。
        DidRecordEntity creatorDid = didRecordMapper.selectByAccountId(order.getCreatorAccountId());
        DidRecordEntity buyerDid = didRecordMapper.selectByAccountId(order.getBuyerAccountId());

        if (creatorDid == null || !"DID_ACTIVE".equals(creatorDid.getStatus())) {
            log.error("授权方无有效DID，无法上链: creatorAccountId={}", order.getCreatorAccountId());
            license.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            licenseMapper.updateById(license);
            markOrderAsGrantException(order, "授权方缺少有效 DID，无法完成授权上链");
            return;
        }
        if (buyerDid == null || !"DID_ACTIVE".equals(buyerDid.getStatus())) {
            log.error("被授权方无有效DID，无法上链: buyerAccountId={}", order.getBuyerAccountId());
            license.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            licenseMapper.updateById(license);
            markOrderAsGrantException(order, "被授权方缺少有效 DID，无法完成授权上链");
            return;
        }

        String licensorDidValue = creatorDid.getDidValue();
        String licenseeDidValue = buyerDid.getDidValue();

        // 5. 调用链码登记授权
        try {
            ChainSubmitResult chainResult = licenseChainAdapter.registerLicense(
                    license.getId(), license.getLicenseNo(), license.getWorkNo(),
                    licensorDidValue,
                    licenseeDidValue,
                    license.getLicenseType(), license.getLicenseHash(), effectiveTime);

            if (chainResult.isSuccess()) {
                // 链提交成功 → CHAIN_SUBMITTED，等待回执确认后再推进到 LICENSE_ACTIVE
                license.setChainStatus(ChainStatusEnum.CHAIN_SUBMITTED.getCode());
                license.setTxHash(chainResult.getTxHash());
                license.setBlockHeight(chainResult.getBlockHeight());
                licenseMapper.updateById(license);

                // 订单先到 AUTH_GRANTING 等待回执
                auditService.writeStatusHistory(BizTypeEnum.LICENSE.getCode(), license.getId(), license.getLicenseNo(),
                        LicenseStatusEnum.LICENSE_PENDING.getCode(), LicenseStatusEnum.LICENSE_PENDING.getCode(),
                        "授权链上交易已提交，等待回执确认", null, null);

                log.info("授权链上交易已提交待回执: licenseNo={}, orderNo={}, txHash={}",
                        license.getLicenseNo(), order.getOrderNo(), chainResult.getTxHash());

                // 通知买家授权上链处理中
                notificationService.sendNotice(order.getBuyerAccountId(), "授权处理中",
                        "您购买的作品 " + order.getWorkNo() + " 授权已提交区块链，交易哈希：" + chainResult.getTxHash() + "，请等待确认。",
                        "LICENSE", "ORDER", order.getOrderNo());
            } else {
                // 链上失败：标记授权链上状态失败
                license.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
                licenseMapper.updateById(license);
                markOrderAsGrantException(order, "授权上链失败: " + chainResult.getFailReason());
                log.error("授权上链失败: licenseNo={}, reason={}",
                        license.getLicenseNo(), chainResult.getFailReason());
            }
        } catch (Exception e) {
            license.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
            licenseMapper.updateById(license);
            markOrderAsGrantException(order, "授权上链异常: " + e.getMessage());
            log.error("授权上链异常: licenseNo={}", license.getLicenseNo(), e);
        }
    }

    /**
     * 按支付渠道解析对应的回调地址。
     */
    private String resolveNotifyUrl(String payChannel) {
        if (PayChannelEnum.WECHAT_PAY.getCode().equals(payChannel)) {
            return wechatNotifyUrl;
        }
        if (PayChannelEnum.ALIPAY.getCode().equals(payChannel)) {
            return alipayNotifyUrl;
        }
        throw new BizException(ErrorCodeEnum.PARAM_INVALID, "不支持的支付渠道: " + payChannel);
    }

    /**
     * 将授权处理中失败的订单统一收敛到异常状态。
     */
    private void markOrderAsGrantException(TradeOrderEntity order, String reason) {
        if (!OrderStatusEnum.AUTH_GRANTING.getCode().equals(order.getOrderStatus())) {
            return;
        }
        order.setOrderStatus(OrderStatusEnum.ORDER_EXCEPTION.getCode());
        orderMapper.updateById(order);
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                OrderStatusEnum.AUTH_GRANTING.getCode(), OrderStatusEnum.ORDER_EXCEPTION.getCode(),
                reason, null, null);
        notificationService.sendNotice(order.getBuyerAccountId(), "授权处理异常",
                "您的订单 " + order.getOrderNo() + " 已支付成功，但授权处理出现异常，请联系平台处理。",
                "ORDER", "ORDER", order.getOrderNo());
        notificationService.sendNotice(order.getCreatorAccountId(), "订单授权异常",
                "订单 " + order.getOrderNo() + " 在授权处理阶段出现异常，请及时关注。",
                "ORDER", "ORDER", order.getOrderNo());
    }

    /**
     * 交易前校验参与授权上链的主体 DID 是否已生效。
     */
    private DidRecordEntity assertActiveDidForTrade(Long accountId, String roleName) {
        DidRecordEntity didRecord = didRecordMapper.selectByAccountId(accountId);
        if (didRecord == null || !DidStatusEnum.DID_ACTIVE.getCode().equals(didRecord.getStatus())
                || didRecord.getSubjectId() == null) {
            throw new BizException(ErrorCodeEnum.DID_NOT_ACTIVE, roleName + "DID未生效，当前无法完成交易授权");
        }
        return didRecord;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderDetailVO getOrderDetail(String orderNo, Long viewerAccountId) {
        TradeOrderEntity order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        // 归属校验：仅买方、卖方或管理员/监管员可查看
        boolean isBuyer = order.getBuyerAccountId().equals(viewerAccountId);
        boolean isCreator = order.getCreatorAccountId().equals(viewerAccountId);
        if (!isBuyer && !isCreator && !FieldVisibilityUtil.isPrivilegedViewer()) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "无权查看该订单详情");
        }
        return buildOrderDetailVO(order);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 支持按角色（BUYER/CREATOR）和状态筛选，按创建时间倒序排列。
     * </p>
     */
    @Override
    public PageResult<OrderListVO> listMyOrders(Long accountId, String role, String status, PageQuery query) {
        Page<TradeOrderEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<TradeOrderEntity> wrapper = new LambdaQueryWrapper<>();
        if ("BUYER".equals(role)) {
            wrapper.eq(TradeOrderEntity::getBuyerAccountId, accountId);
        } else if ("CREATOR".equals(role)) {
            wrapper.eq(TradeOrderEntity::getCreatorAccountId, accountId);
        } else {
            // 未识别的 role 默认按买方查询，不允许无过滤查询
            wrapper.eq(TradeOrderEntity::getBuyerAccountId, accountId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(TradeOrderEntity::getOrderStatus, status);
        }
        wrapper.orderByDesc(TradeOrderEntity::getCreatedAt);
        page = orderMapper.selectPage(page, wrapper);

        List<OrderListVO> vos = page.getRecords().stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            vo.setOrderNo(o.getOrderNo());
            WorkEntity work = workMapper.selectByWorkNo(o.getWorkNo());
            vo.setWorkTitle(work != null ? work.getTitle() : o.getWorkNo());
            vo.setOrderStatus(o.getOrderStatus());
            vo.setPayAmount(o.getPayAmount());
            vo.setPayChannel(o.getPayChannel());
            vo.setCreatedAt(o.getCreatedAt());
            return vo;
        }).toList();
        return PageResult.of(vos, page.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 仅 ORDER_CREATED 和 PAY_PENDING_CONFIRM 状态可取消。
     * 取消时同步关闭待支付的支付记录。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long buyerAccountId, String orderNo) {
        log.info("取消订单，buyerAccountId={}, orderNo={}", buyerAccountId, orderNo);

        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED);
        }

        if (!OrderStatusEnum.ORDER_CREATED.getCode().equals(order.getOrderStatus())
                && !OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(order.getOrderStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "当前订单状态不允许取消",
                    null, order.getOrderStatus());
        }

        if (PayChannelEnum.ALIPAY.getCode().equals(order.getPayChannel())
                && OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(order.getOrderStatus())) {
            var adapter = paymentAdapterFactory.getQueryableAdapter(order.getPayChannel());
            PaymentQueryResult query = adapter.queryPayment(orderNo);
            if (query.isPaid()) {
                throw new BizException(ErrorCodeEnum.ORDER_ALREADY_PAID, "订单已付款，请刷新支付状态");
            }
            if (!Set.of("WAIT_BUYER_PAY", "TRADE_CLOSED", "NOT_FOUND").contains(query.getTradeStatus())) {
                throw new BizException(ErrorCodeEnum.PAY_CONFIRM_FAILED, "付款结果尚未确认，请稍后再取消");
            }
            adapter.closePayment(orderNo);
        }

        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatusEnum.ORDER_CANCELLED.getCode());
        order.setPayStatus(PayStatusEnum.PAY_CLOSED.getCode());
        order.setCancelTime(DateTimeUtil.nowUtc());
        order.setCancelReason("用户主动取消");
        orderMapper.updateById(order);

        // 关闭待支付的支付记录
        List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
        for (PaymentRecordEntity payment : payments) {
            if (PayStatusEnum.PAY_PENDING.getCode().equals(payment.getPayStatus())) {
                payment.setPayStatus(PayStatusEnum.PAY_CLOSED.getCode());
                paymentMapper.updateById(payment);
            }
        }

        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                fromStatus, OrderStatusEnum.ORDER_CANCELLED.getCode(),
                "用户取消订单", null, buyerAccountId);

        traceEventService.writeTraceEvent(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                "ORDER_CANCELLED", "订单已取消", buyerAccountId, null, null);

        log.info("订单取消成功: orderNo={}", orderNo);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 允许退款的订单状态：AUTH_GRANTED、ORDER_COMPLETED、SETTLEMENT_PENDING。
     * 创建退款记录后订单状态变为 REFUND_PENDING，等待管理员审批。
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long buyerAccountId, String orderNo, RefundApplyRequest request) {
        log.info("申请退款，buyerAccountId={}, orderNo={}", buyerAccountId, orderNo);

        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED);
        }

        // 校验允许退款的状态
        String orderStatus = order.getOrderStatus();
        if (!OrderStatusEnum.AUTH_GRANTED.getCode().equals(orderStatus)
                && !OrderStatusEnum.ORDER_COMPLETED.getCode().equals(orderStatus)
                && !OrderStatusEnum.SETTLEMENT_PENDING.getCode().equals(orderStatus)
                && !OrderStatusEnum.AUTH_GRANTING.getCode().equals(orderStatus)
                && !OrderStatusEnum.PAY_CONFIRMED.getCode().equals(orderStatus)
                && !OrderStatusEnum.ORDER_EXCEPTION.getCode().equals(orderStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "当前订单状态不允许退款");
        }

        // 获取支付记录
        List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
        PaymentRecordEntity payment = payments.stream()
                .filter(p -> PayStatusEnum.PAY_SUCCESS.getCode().equals(p.getPayStatus()))
                .findFirst().orElseThrow(() -> new BizException(ErrorCodeEnum.REFUND_FAILED, "缺少已付款记录"));
        boolean reservedRefund = refundMapper.selectByOrderId(order.getId()).stream()
                .anyMatch(r -> Set.of(RefundStatusEnum.PENDING.getCode(), RefundStatusEnum.PROCESSING.getCode(), RefundStatusEnum.SUCCESS.getCode()).contains(r.getRefundStatus()));
        if (reservedRefund) {
            throw new BizException(ErrorCodeEnum.REFUND_FAILED, "订单已有待处理或已成功的退款");
        }

        LocalDateTime now = DateTimeUtil.nowUtc();
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundNo(BizNoUtil.refundNo());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setPaymentId(payment.getId());
        refund.setPaymentNo(payment.getPaymentNo());
        refund.setPayChannel(payment.getPayChannel());
        refund.setRefundAmount(order.getPayAmount());
        refund.setCurrency(order.getCurrency());
        refund.setRefundStatus(RefundStatusEnum.PENDING.getCode());
        refund.setRefundReason(request.getReason());
        refund.setApplyTime(now);
        refund.setRequestId(request.getRequestId());
        refund.setPreRefundOrderStatus(order.getOrderStatus());
        refundMapper.insert(refund);

        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatusEnum.REFUND_PENDING.getCode());
        orderMapper.updateById(order);

        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                fromStatus, OrderStatusEnum.REFUND_PENDING.getCode(),
                "申请退款:" + request.getReason(), null, buyerAccountId);

        traceEventService.writeTraceEvent(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                "ORDER_REFUND_APPLIED", "退款申请已提交", buyerAccountId, null, null);

        log.info("退款申请创建成功: refundNo={}, orderNo={}", refund.getRefundNo(), order.getOrderNo());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 管理员审批退款：
     * <ul>
     *   <li>APPROVE：调用支付渠道退款接口，成功则订单状态变为 REFUNDED</li>
     *   <li>REJECT：退款状态设为 REJECTED，订单恢复到申请前状态</li>
     * </ul>
     * </p>
     */
    @Override
    public void processRefund(Long operatorId, RefundProcessRequest request) {
        // 先提交退款意图，再调用外部渠道。进程中断或本地回写失败时，定时查询仍能找回结果。
        var transaction = new TransactionTemplate(transactionManager);
        RefundRequest refundRequest = transaction.execute(status -> prepareRefund(operatorId, request));
        if (refundRequest == null) return;
        RefundResponse response = paymentAdapterFactory.refund(refundRequest);
        if (response.isPending()) return;
        transaction.executeWithoutResult(status -> {
            TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(refundRequest.getOrderNo());
            RefundRecordEntity refund = refundMapper.selectByRefundNo(refundRequest.getRefundNo());
            if (RefundStatusEnum.PROCESSING.getCode().equals(refund.getRefundStatus())) {
                if (!response.isSuccess()) {
                    failRefund(order, refund, response.getErrorMsg());
                    return;
                }
                refund.setThirdRefundNo(response.getThirdRefundNo());
                completeRefund(order, refund, operatorId);
            }
        });
    }

    private RefundRequest prepareRefund(Long operatorId, RefundProcessRequest request) {
        RefundRecordEntity refund = refundMapper.selectByRefundNo(request.getRefundNo());
        if (refund == null) throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "退款记录不存在");
        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(refund.getOrderNo());
        if (order == null) throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        refund = refundMapper.selectByRefundNo(request.getRefundNo());
        if (!RefundStatusEnum.PENDING.getCode().equals(refund.getRefundStatus())
                || !OrderStatusEnum.REFUND_PENDING.getCode().equals(order.getOrderStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "退款状态不允许审批");
        }
        if (!Set.of("APPROVE", "REJECT").contains(request.getAction())) {
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "退款审批动作无效");
        }
        refund.setOperatorId(operatorId);
        if ("REJECT".equals(request.getAction())) {
            if (refund.getPreRefundOrderStatus() == null) {
                throw new BizException(ErrorCodeEnum.STATUS_INVALID, "退款缺少原订单状态");
            }
            refund.setRefundStatus(RefundStatusEnum.REJECTED.getCode());
            refundMapper.updateById(refund);
            order.setOrderStatus(refund.getPreRefundOrderStatus());
            orderMapper.updateById(order);
            auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                    OrderStatusEnum.REFUND_PENDING.getCode(), order.getOrderStatus(),
                    "退款被拒绝" + (request.getComment() == null ? "" : ":" + request.getComment()), null, operatorId);
            return null;
        }
        PaymentRecordEntity payment = paymentMapper.selectById(refund.getPaymentId());
        if (payment == null || !PayStatusEnum.PAY_SUCCESS.getCode().equals(payment.getPayStatus())
                || !order.getId().equals(payment.getOrderId()) || !order.getPayChannel().equals(payment.getPayChannel())
                || !java.util.Objects.equals(refund.getRefundAmount(), order.getPayAmount())
                || !java.util.Objects.equals(payment.getPayAmount(), order.getPayAmount())
                || !java.util.Objects.equals(payment.getCurrency(), order.getCurrency())) {
            throw new BizException(ErrorCodeEnum.REFUND_FAILED, "退款原支付记录或金额不匹配");
        }
        RefundRequest refundRequest = RefundRequest.builder().refundNo(refund.getRefundNo())
                .orderNo(order.getOrderNo()).thirdTradeNo(payment.getThirdTradeNo())
                .payChannel(payment.getPayChannel()).refundAmount(refund.getRefundAmount())
                .totalAmount(order.getPayAmount()).reason(refund.getRefundReason()).build();
        MoneyUtil.requireFen(refund.getRefundAmount());
        MoneyUtil.requireCny(order.getCurrency());
        refund.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());
        refundMapper.updateById(refund);
        auditService.writeAuditLog(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                "REFUND_APPROVED", request.getComment(), operatorId, null, null, "PROCESSING", null);
        return refundRequest;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundDetailVO refreshRefund(Long operatorId, String refundNo) {
        RefundRecordEntity refund = refundMapper.selectByRefundNo(refundNo);
        if (refund == null) throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "退款记录不存在");
        TradeOrderEntity order = orderMapper.selectByOrderNoForUpdate(refund.getOrderNo());
        if (order == null) throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        refund = refundMapper.selectByRefundNo(refundNo);
        if (RefundStatusEnum.PROCESSING.getCode().equals(refund.getRefundStatus())) {
            if (refund.getUpdatedAt() != null && refund.getUpdatedAt().plusSeconds(10).isAfter(DateTimeUtil.nowUtc())) {
                throw new BizException(ErrorCodeEnum.STATUS_INVALID, "请在退款提交至少 10 秒后查询");
            }
            RefundResponse response = paymentAdapterFactory.getAdapter(refund.getPayChannel())
                    .queryRefund(order.getOrderNo(), refund.getRefundNo(), refund.getRefundAmount());
            if (!response.isSuccess() && !response.isPending()) {
                failRefund(order, refund, response.getErrorMsg());
                return getRefundDetail(refundNo);
            }
            if (response.isPending()) {
                // 覆盖“已提交退款意图，但进程在调用渠道前退出”的窗口；重试固定out_request_no，绝不新建退款编号。
                var payment = paymentMapper.selectById(refund.getPaymentId());
                if (payment == null || !PayStatusEnum.PAY_SUCCESS.getCode().equals(payment.getPayStatus())) {
                    throw new BizException(ErrorCodeEnum.REFUND_FAILED, "退款原支付记录不可用");
                }
                response = paymentAdapterFactory.refund(RefundRequest.builder().refundNo(refund.getRefundNo())
                        .orderNo(order.getOrderNo()).thirdTradeNo(payment.getThirdTradeNo()).payChannel(refund.getPayChannel())
                        .totalAmount(order.getPayAmount()).refundAmount(refund.getRefundAmount()).reason(refund.getRefundReason()).build());
                // 已存在的退款可能仍在处理；重试时返回拒绝也不能抹掉之前的未知资金事实，继续查询。
            }
            if (response.isSuccess() && !response.isPending()) {
                refund.setThirdRefundNo(response.getThirdRefundNo());
                completeRefund(order, refund, operatorId);
            }
        }
        return getRefundDetail(refundNo);
    }

    private void failRefund(TradeOrderEntity order, RefundRecordEntity refund, String reason) {
        refund.setRefundStatus(RefundStatusEnum.FAILED.getCode());
        refund.setFailReason(reason);
        refundMapper.updateById(refund);
        order.setOrderStatus(refund.getPreRefundOrderStatus());
        orderMapper.updateById(order);
    }

    /** 渠道退款事实独立提交；后续逆分账失败不能回滚已确认的退款。 */
    private void completeRefund(TradeOrderEntity order, RefundRecordEntity refund, Long operatorId) {
        if (RefundStatusEnum.SUCCESS.getCode().equals(refund.getRefundStatus())) return;
        refund.setRefundStatus(RefundStatusEnum.SUCCESS.getCode());
        refund.setCompleteTime(DateTimeUtil.nowUtc());
        if (operatorId != null) refund.setOperatorId(operatorId);
        refundMapper.updateById(refund);
        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
        orderMapper.updateById(order);
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                fromStatus, order.getOrderStatus(), "退款已由支付渠道确认", null, operatorId);
        revokeRefundedLicense(order);
    }

    private void revokeRefundedLicense(TradeOrderEntity order) {
        var license = licenseMapper.selectByOrderId(order.getId());
        if (license != null && !LicenseStatusEnum.LICENSE_REVOKED.getCode().equals(license.getLicenseStatus())) {
            String fromStatus = license.getLicenseStatus();
            license.setLicenseStatus(LicenseStatusEnum.LICENSE_REVOKED.getCode());
            license.setChainStatus(ChainStatusEnum.CHAIN_PENDING.getCode());
            license.setTxHash(null);
            license.setBlockHeight(null);
            licenseMapper.updateById(license);
            auditService.writeStatusHistory(BizTypeEnum.LICENSE.getCode(), license.getId(), license.getLicenseNo(),
                    fromStatus, license.getLicenseStatus(), "退款确认，撤销业务授权", null, null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reconcileRefundEffects(String refundNo) {
        var refund = refundMapper.selectByRefundNo(refundNo);
        if (refund == null) return;
        var order = orderMapper.selectByOrderNoForUpdate(refund.getOrderNo());
        refund = refundMapper.selectByRefundNo(refundNo);
        if (order == null || !RefundStatusEnum.SUCCESS.getCode().equals(refund.getRefundStatus())) return;
        revokeRefundedLicense(order);
        var license = licenseMapper.selectByOrderId(order.getId());
        if (license != null && Set.of(ChainStatusEnum.CHAIN_PENDING.getCode(),
                ChainStatusEnum.CHAIN_FAILED.getCode()).contains(license.getChainStatus())) {
            var result = licenseChainAdapter.revokeLicense(license.getId(), license.getLicenseNo(),
                    "订单退款确认", refund.getCompleteTime());
            license.setChainStatus(result.isSuccess() ? ChainStatusEnum.CHAIN_SUBMITTED.getCode() : ChainStatusEnum.CHAIN_FAILED.getCode());
            license.setTxHash(result.getTxHash());
            license.setBlockHeight(result.getBlockHeight());
            licenseMapper.updateById(license);
        }
        var settlement = settlementRecordMapper.selectByOrderNo(order.getOrderNo());
        // 结算回执晚于退款时也会进入此处；逆分账复用原有编号，失败可重试。
        if (settlement != null && Set.of(SettlementStatusEnum.SETTLE_SUCCESS.getCode(),
                SettlementStatusEnum.REVERSE_PENDING.getCode()).contains(settlement.getStatus())) {
            settlementService.reverseSettlement(settlement.getSettleNo(), "退款触发逆分账");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSettlement(String orderNo) {
        var order = orderMapper.selectByOrderNoForUpdate(orderNo);
        if (order == null || !OrderStatusEnum.SETTLEMENT_PENDING.getCode().equals(order.getOrderStatus())) return;
        var settlement = settlementRecordMapper.selectByOrderNo(orderNo);
        if (settlement == null || !SettlementStatusEnum.SETTLE_SUCCESS.getCode().equals(settlement.getStatus())) return;
        order.setOrderStatus(OrderStatusEnum.ORDER_COMPLETED.getCode());
        order.setCompleteTime(DateTimeUtil.nowUtc());
        orderMapper.updateById(order);
        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                OrderStatusEnum.SETTLEMENT_PENDING.getCode(), order.getOrderStatus(), "结算回执确认，订单完成", null, null);
    }

    /**
     * 构建订单详情视图对象
     *
     * @param order 订单实体
     * @return 订单详情VO
     */
    private OrderDetailVO buildOrderDetailVO(TradeOrderEntity order) {
        OrderDetailVO vo = new OrderDetailVO();
        WorkEntity work = workMapper.selectById(order.getWorkId());

        var basic = new OrderDetailVO.BasicInfo();
        basic.setOrderNo(order.getOrderNo());
        basic.setWorkTitle(work != null ? work.getTitle() : null);
        basic.setLicenseType(order.getLicenseType());
        basic.setPriceAmount(order.getPriceAmount());
        basic.setPayAmount(order.getPayAmount());
        basic.setCurrency(order.getCurrency());
        basic.setPayChannel(order.getPayChannel());
        vo.setBasicInfo(basic);

        var status = new OrderDetailVO.StatusInfo();
        status.setOrderStatus(order.getOrderStatus());
        status.setPayStatus(order.getPayStatus());
        vo.setStatusInfo(status);

        var time = new OrderDetailVO.TimeInfo();
        time.setCreatedAt(order.getCreatedAt());
        time.setExpireTime(order.getExpireTime());
        time.setPayTime(order.getPayTime());
        time.setCompleteTime(order.getCompleteTime());
        vo.setTimeInfo(time);

        var relation = new OrderDetailVO.RelationInfo();
        relation.setWorkNo(order.getWorkNo());
        relation.setListingNo(order.getListingNo());
        if (order.getBuyerAccountId() != null) {
            AccountEntity buyer = accountMapper.selectById(order.getBuyerAccountId());
            relation.setBuyerAccountNo(buyer != null ? buyer.getAccountNo() : null);
        }
        if (order.getCreatorAccountId() != null) {
            AccountEntity creator = accountMapper.selectById(order.getCreatorAccountId());
            relation.setCreatorAccountNo(creator != null ? creator.getAccountNo() : null);
        }
        vo.setRelationInfo(relation);

        // 查询关联授权
        LicenseRecordEntity license = licenseMapper.selectByOrderId(order.getId());
        if (license != null) {
            relation.setLicenseNo(license.getLicenseNo());
        }
        var settlement = settlementRecordMapper.selectByOrderNo(order.getOrderNo());
        if (settlement != null) {
            relation.setSettleNo(settlement.getSettleNo());
        }

        vo.setChainInfo(new OrderDetailVO.ChainInfo());

        // 计算可执行动作
        vo.setAllowedActions(computeAllowedActions(order));

        // 统一可见性装配
        TradeVoAssembler.applyVisibility(vo);

        return vo;
    }

    /**
     * 根据订单当前状态计算允许执行的操作列表
     *
     * @param order 订单实体
     * @return 允许的操作编码列表
     */
    private List<String> computeAllowedActions(TradeOrderEntity order) {
        List<String> actions = new ArrayList<>();
        if (!order.getBuyerAccountId().equals(UserContext.getUserId())) return actions;
        String status = order.getOrderStatus();
        if (OrderStatusEnum.ORDER_CREATED.getCode().equals(status)) {
            actions.add("PAY");
            actions.add("CANCEL");
        }
        if (OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(status)) {
            if (PayChannelEnum.ALIPAY.getCode().equals(order.getPayChannel())) {
                actions.add("PAY");
                actions.add("QUERY_PAYMENT");
            }
            actions.add("CANCEL");
        }
        if (OrderStatusEnum.AUTH_GRANTED.getCode().equals(status)
                || OrderStatusEnum.ORDER_COMPLETED.getCode().equals(status)
                || OrderStatusEnum.SETTLEMENT_PENDING.getCode().equals(status)
                || (PayStatusEnum.PAY_SUCCESS.getCode().equals(order.getPayStatus())
                    && Set.of("AUTH_GRANTING", "PAY_CONFIRMED", "ORDER_EXCEPTION").contains(status))) {
            actions.add("APPLY_REFUND");
        }
        return actions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<RefundDetailVO> listRefunds(String status, PageQuery query) {
        Page<RefundRecordEntity> page = new Page<>(query.getPageNo(), query.getPageSize());
        LambdaQueryWrapper<RefundRecordEntity> wrapper = new LambdaQueryWrapper<RefundRecordEntity>()
                .eq(status != null && !status.isBlank(), RefundRecordEntity::getRefundStatus, status)
                .orderByDesc(RefundRecordEntity::getApplyTime);

        Page<RefundRecordEntity> result = refundMapper.selectPage(page, wrapper);

        List<RefundDetailVO> voList = result.getRecords().stream()
                .map(this::toRefundDetailVO)
                .toList();
        return PageResult.of(voList, result.getTotal(), query.getPageNo(), query.getPageSize());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RefundDetailVO getRefundDetail(String refundNo) {
        RefundRecordEntity refund = refundMapper.selectByRefundNo(refundNo);
        if (refund == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "退款记录不存在: " + refundNo);
        }
        return toRefundDetailVO(refund);
    }

    private RefundDetailVO toRefundDetailVO(RefundRecordEntity entity) {
        RefundDetailVO vo = new RefundDetailVO();
        vo.setRefundNo(entity.getRefundNo());
        vo.setOrderNo(entity.getOrderNo());
        vo.setPaymentNo(entity.getPaymentNo());
        vo.setPayChannel(entity.getPayChannel());
        vo.setRefundAmount(entity.getRefundAmount());
        vo.setCurrency(entity.getCurrency());
        vo.setRefundStatus(entity.getRefundStatus());
        vo.setRefundReason(entity.getRefundReason());
        vo.setThirdRefundNo(entity.getThirdRefundNo());
        vo.setApplyTime(entity.getApplyTime());
        vo.setCompleteTime(entity.getCompleteTime());
        vo.setFailReason(entity.getFailReason());
        return vo;
    }

    /**
     * {@inheritDoc}
     * <p>
     * 不按当前登录用户归属过滤，按管理员筛选条件分页查询全部订单。
     * 默认按创建时间倒序排列。
     * </p>
     */
    @Override
    public PageResult<AdminOrderListVO> listAdminOrders(AdminOrderQuery adminQuery, PageQuery pageQuery) {
        // 如果查询条件包含 accountNo，先解析为 accountId
        Long buyerAccountId = resolveAccountId(adminQuery.getBuyerAccountNo());
        Long creatorAccountId = resolveAccountId(adminQuery.getCreatorAccountNo());

        // 如果提供了 accountNo 但解析失败（用户不存在），直接返回空结果
        if (adminQuery.getBuyerAccountNo() != null && !adminQuery.getBuyerAccountNo().isBlank() && buyerAccountId == null) {
            return PageResult.of(List.of(), 0, pageQuery.getPageNo(), pageQuery.getPageSize());
        }
        if (adminQuery.getCreatorAccountNo() != null && !adminQuery.getCreatorAccountNo().isBlank() && creatorAccountId == null) {
            return PageResult.of(List.of(), 0, pageQuery.getPageNo(), pageQuery.getPageSize());
        }

        // 如果查询条件包含 licenseNo，先解析为 orderId
        Long filterOrderId = null;
        if (adminQuery.getLicenseNo() != null && !adminQuery.getLicenseNo().isBlank()) {
            LicenseRecordEntity licenseRecord = licenseMapper.selectByLicenseNo(adminQuery.getLicenseNo());
            if (licenseRecord == null) {
                return PageResult.of(List.of(), 0, pageQuery.getPageNo(), pageQuery.getPageSize());
            }
            filterOrderId = licenseRecord.getOrderId();
        }

        Page<TradeOrderEntity> page = new Page<>(pageQuery.getPageNo(), pageQuery.getPageSize());
        LambdaQueryWrapper<TradeOrderEntity> wrapper = new LambdaQueryWrapper<>();

        if (filterOrderId != null) {
            wrapper.eq(TradeOrderEntity::getId, filterOrderId);
        }
        if (adminQuery.getOrderNo() != null && !adminQuery.getOrderNo().isBlank()) {
            wrapper.like(TradeOrderEntity::getOrderNo, adminQuery.getOrderNo());
        }
        if (buyerAccountId != null) {
            wrapper.eq(TradeOrderEntity::getBuyerAccountId, buyerAccountId);
        }
        if (creatorAccountId != null) {
            wrapper.eq(TradeOrderEntity::getCreatorAccountId, creatorAccountId);
        }
        if (adminQuery.getOrderStatus() != null && !adminQuery.getOrderStatus().isBlank()) {
            wrapper.eq(TradeOrderEntity::getOrderStatus, adminQuery.getOrderStatus());
        }
        if (adminQuery.getPayChannel() != null && !adminQuery.getPayChannel().isBlank()) {
            wrapper.eq(TradeOrderEntity::getPayChannel, adminQuery.getPayChannel());
        }
        if (adminQuery.getWorkNo() != null && !adminQuery.getWorkNo().isBlank()) {
            wrapper.eq(TradeOrderEntity::getWorkNo, adminQuery.getWorkNo());
        }
        if (adminQuery.getDateFrom() != null) {
            wrapper.ge(TradeOrderEntity::getCreatedAt, adminQuery.getDateFrom());
        }
        if (adminQuery.getDateTo() != null) {
            wrapper.le(TradeOrderEntity::getCreatedAt, adminQuery.getDateTo());
        }
        wrapper.orderByDesc(TradeOrderEntity::getCreatedAt);

        page = orderMapper.selectPage(page, wrapper);

        List<AdminOrderListVO> voList = page.getRecords().stream()
                .map(this::buildAdminOrderListVO)
                .toList();

        return PageResult.of(voList, page.getTotal(), pageQuery.getPageNo(), pageQuery.getPageSize());
    }

    private AdminOrderListVO buildAdminOrderListVO(TradeOrderEntity order) {
        // 查询关联授权编号
        LicenseRecordEntity license = licenseMapper.selectByOrderId(order.getId());
        String licenseNo = license != null ? license.getLicenseNo() : null;

        AdminOrderListVO vo = new AdminOrderListVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setWorkNo(order.getWorkNo());
        vo.setListingNo(order.getListingNo());
        vo.setLicenseNo(licenseNo);
        vo.setPayChannel(order.getPayChannel());
        vo.setPayAmount(order.getPayAmount());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setUpdatedAt(order.getUpdatedAt());

        // 解析买方/创作者账户编号
        if (order.getBuyerAccountId() != null) {
            AccountEntity buyer = accountMapper.selectById(order.getBuyerAccountId());
            vo.setBuyerAccountNo(buyer != null ? buyer.getAccountNo() : null);
        }
        if (order.getCreatorAccountId() != null) {
            AccountEntity creator = accountMapper.selectById(order.getCreatorAccountId());
            vo.setCreatorAccountNo(creator != null ? creator.getAccountNo() : null);
        }

        // 判断是否存在退款
        List<RefundRecordEntity> refunds = refundMapper.selectByOrderId(order.getId());
        vo.setHasRefund(refunds != null && !refunds.isEmpty());

        return vo;
    }

    private Long resolveAccountId(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return null;
        }
        AccountEntity account = accountMapper.selectByAccountNo(accountNo);
        return account != null ? account.getId() : null;
    }
}
