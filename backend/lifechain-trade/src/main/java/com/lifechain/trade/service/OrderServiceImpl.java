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
import com.lifechain.common.enums.*;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.common.util.FieldVisibilityUtil;
import com.lifechain.common.util.HashUtil;
import com.lifechain.infra.notification.NotificationService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final LicenseRecordMapper licenseMapper;
    private final RefundRecordMapper refundMapper;
    private final WorkListingMapper listingMapper;
    private final WorkMapper workMapper;
    private final PaymentAdapterFactory paymentAdapterFactory;
    private final LicenseChainAdapter licenseChainAdapter;
    private final AuditService auditService;
    private final TraceEventService traceEventService;
    private final ObjectMapper objectMapper;
    private final WorkSettleRuleMapper workSettleRuleMapper;
    private final SettlementRecordMapper settlementRecordMapper;
    private final SettlementService settlementService;
    private final NotificationService notificationService;
    private final RedisService redisService;

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
        assertActiveDidForTrade(listing.getCreatorAccountId(), "创作者");
        assertActiveDidForTrade(buyerAccountId, "购买方");

        // 3. 创建订单
        LocalDateTime now = DateTimeUtil.nowUtc();
        TradeOrderEntity order = new TradeOrderEntity();
        order.setOrderNo(BizNoUtil.orderNo());
        order.setWorkId(listing.getWorkId());
        order.setWorkNo(listing.getWorkNo());
        order.setListingId(listing.getId());
        order.setListingNo(listing.getListingNo());
        order.setBuyerAccountId(buyerAccountId);
        order.setCreatorAccountId(listing.getCreatorAccountId());
        order.setOrderStatus(OrderStatusEnum.ORDER_CREATED.getCode());
        order.setLicenseType(listing.getLicenseType());
        order.setPriceAmount(listing.getPriceAmount());
        order.setPayAmount(listing.getPriceAmount());
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
            WorkSettleRuleEntity settleRule = workSettleRuleMapper.selectEffectiveRuleByWorkNo(listing.getWorkNo());
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
    public PayResultVO payOrder(Long buyerAccountId, PayRequest request) {
        log.info("发起支付，buyerAccountId={}, orderNo={}", buyerAccountId, request.getOrderNo());

        // 1. 查询订单
        TradeOrderEntity order = orderMapper.selectByOrderNo(request.getOrderNo());
        if (order == null) {
            throw new BizException(ErrorCodeEnum.ORDER_NOT_FOUND);
        }
        if (!buyerAccountId.equals(order.getBuyerAccountId())) {
            throw new BizException(ErrorCodeEnum.RESOURCE_ACCESS_DENIED, "非本人订单");
        }

        // 2. 校验订单状态
        if (!OrderStatusEnum.ORDER_CREATED.getCode().equals(order.getOrderStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "订单状态不允许支付",
                    null, order.getOrderStatus());
        }

        // 3. 校验是否过期
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(DateTimeUtil.nowUtc())) {
            order.setOrderStatus(OrderStatusEnum.ORDER_EXPIRED.getCode());
            orderMapper.updateById(order);
            auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                    OrderStatusEnum.ORDER_CREATED.getCode(), OrderStatusEnum.ORDER_EXPIRED.getCode(),
                    "订单已过期", null, null);
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
        payReq.setPayChannel(request.getPayChannel());
        payReq.setNotifyUrl(notifyUrl);
        payReq.setClientIp(request.getClientIp());
        payReq.setRequestId(request.getRequestId());

        PaymentResponse payResp = paymentAdapterFactory.createPayment(payReq);
        if (!payResp.isSuccess()) {
            throw new BizException(ErrorCodeEnum.PAY_CREATE_FAILED, payResp.getErrorMsg());
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
    public void handlePaymentCallback(String payChannel, PaymentCallbackResult callbackResult) {
        String orderNo = callbackResult.getOrderNo();
        log.info("处理支付回调: orderNo={}, channel={}, success={}",
                orderNo, payChannel, callbackResult.isSuccess());

        // 1. 查询订单
        TradeOrderEntity order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.error("支付回调订单不存在: orderNo={}", orderNo);
            return;
        }

        // 2. 幂等检查：如果订单已进入支付确认及之后的状态，直接返回
        String currentStatus = order.getOrderStatus();
        if (OrderStatusEnum.PAY_CONFIRMED.getCode().equals(currentStatus)
                || OrderStatusEnum.AUTH_GRANTING.getCode().equals(currentStatus)
                || OrderStatusEnum.AUTH_GRANTED.getCode().equals(currentStatus)
                || OrderStatusEnum.SETTLEMENT_PENDING.getCode().equals(currentStatus)
                || OrderStatusEnum.ORDER_COMPLETED.getCode().equals(currentStatus)) {
            log.info("支付回调幂等处理，订单已处理: orderNo={}, currentStatus={}", orderNo, currentStatus);
            return;
        }

        // 2.5 校验支付渠道一致性：回调渠道必须与订单原支付渠道一致
        if (order.getPayChannel() != null && !order.getPayChannel().equals(payChannel)) {
            log.error("支付渠道不一致: orderNo={}, orderChannel={}, callbackChannel={}",
                    orderNo, order.getPayChannel(), payChannel);
            order.setOrderStatus(OrderStatusEnum.ORDER_EXCEPTION.getCode());
            orderMapper.updateById(order);
            auditService.writeAuditLog(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                    "PAY_CHANNEL_MISMATCH",
                    "支付渠道不一致,订单渠道:" + order.getPayChannel() + ",回调渠道:" + payChannel,
                    null, null, null, "FAILED", "PAY_CHANNEL_MISMATCH");
            return;
        }

        // 3. 校验金额一致性
        if (callbackResult.isSuccess() && callbackResult.getPaidAmount().compareTo(order.getPayAmount()) != 0) {
            log.error("支付金额不一致: orderNo={}, expected={}, actual={}",
                    orderNo, order.getPayAmount(), callbackResult.getPaidAmount());
            order.setOrderStatus(OrderStatusEnum.ORDER_EXCEPTION.getCode());
            orderMapper.updateById(order);
            auditService.writeAuditLog(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                    "PAY_AMOUNT_MISMATCH",
                    "支付金额不一致,期望:" + order.getPayAmount() + ",实际:" + callbackResult.getPaidAmount(),
                    null, null, null, "FAILED", ErrorCodeEnum.PAY_AMOUNT_MISMATCH.getCode());
            return;
        }

        // 4. 更新支付记录
        LocalDateTime now = DateTimeUtil.nowUtc();
        List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
        PaymentRecordEntity payment = payments.isEmpty() ? null : payments.get(0);
        if (payment != null) {
            payment.setThirdTradeNo(callbackResult.getThirdTradeNo());
            payment.setPayTime(callbackResult.getPayTime());
            payment.setCallbackTime(now);
            payment.setCallbackRawRef(callbackResult.getRawPayload() != null
                    ? HashUtil.sha256(callbackResult.getRawPayload()) : null);
            if (callbackResult.isSuccess()) {
                payment.setPayStatus(PayStatusEnum.PAY_SUCCESS.getCode());
            } else {
                payment.setPayStatus(PayStatusEnum.PAY_FAILED.getCode());
            }
            paymentMapper.updateById(payment);
        }

        // 5. 支付失败处理
        if (!callbackResult.isSuccess()) {
            log.warn("支付失败: orderNo={}", orderNo);
            return;
        }

        // 6. 支付成功：更新订单状态到 PAY_CONFIRMED
        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatusEnum.PAY_CONFIRMED.getCode());
        order.setPayStatus(PayStatusEnum.PAY_SUCCESS.getCode());
        order.setPayTime(callbackResult.getPayTime() != null ? callbackResult.getPayTime() : now);
        orderMapper.updateById(order);

        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                fromStatus, OrderStatusEnum.PAY_CONFIRMED.getCode(),
                "支付确认", null, null);
        auditService.writeAuditLog(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                "PAY_CONFIRMED",
                "支付确认,第三方流水号:" + callbackResult.getThirdTradeNo(),
                null, null, null, "SUCCESS", null);

        traceEventService.writeTraceEvent(BizTypeEnum.ORDER.getCode(), order.getId(), orderNo,
                "ORDER_PAY_CONFIRMED", "支付确认成功", null, null, null);

        // 通知买家支付成功
        notificationService.sendNotice(order.getBuyerAccountId(), "支付成功",
                "您的订单 " + orderNo + " 已支付成功，授权正在处理中。",
                "ORDER", "ORDER", orderNo);
        // 通知卖家（创作者）有新的授权购买
        notificationService.sendNotice(order.getCreatorAccountId(), "作品被购买",
                "您的作品 " + order.getWorkNo() + " 已被购买，订单号：" + orderNo + "，授权正在上链处理。",
                "ORDER", "ORDER", orderNo);

        // 7. 自动授权上链
        grantLicense(order);
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

        // 4. 查询授权方与被授权方的真实DID
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
                    license.getLicenseType(), license.getLicenseHash(), now);

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
    private void assertActiveDidForTrade(Long accountId, String roleName) {
        DidRecordEntity didRecord = didRecordMapper.selectByAccountId(accountId);
        if (didRecord == null || !DidStatusEnum.DID_ACTIVE.getCode().equals(didRecord.getStatus())) {
            throw new BizException(ErrorCodeEnum.DID_NOT_ACTIVE, roleName + "DID未生效，当前无法完成交易授权");
        }
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

        TradeOrderEntity order = orderMapper.selectByOrderNo(orderNo);
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

        String fromStatus = order.getOrderStatus();
        order.setOrderStatus(OrderStatusEnum.ORDER_CANCELLED.getCode());
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
    public void applyRefund(Long buyerAccountId, RefundApplyRequest request) {
        log.info("申请退款，buyerAccountId={}, orderNo={}", buyerAccountId, request.getOrderNo());

        TradeOrderEntity order = orderMapper.selectByOrderNo(request.getOrderNo());
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
                && !OrderStatusEnum.SETTLEMENT_PENDING.getCode().equals(orderStatus)) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "当前订单状态不允许退款");
        }

        // 获取支付记录
        List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
        PaymentRecordEntity payment = payments.isEmpty() ? null : payments.get(0);

        LocalDateTime now = DateTimeUtil.nowUtc();
        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundNo(BizNoUtil.refundNo());
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        if (payment != null) {
            refund.setPaymentId(payment.getId());
            refund.setPaymentNo(payment.getPaymentNo());
            refund.setPayChannel(payment.getPayChannel());
        }
        refund.setRefundAmount(order.getPayAmount());
        refund.setCurrency(order.getCurrency());
        refund.setRefundStatus("PENDING");
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
     *   <li>REJECT：退款状态设为 REJECTED，订单恢复到 AUTH_GRANTED</li>
     * </ul>
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processRefund(Long operatorId, RefundProcessRequest request) {
        log.info("处理退款审批，operatorId={}, refundNo={}, action={}",
                operatorId, request.getRefundNo(), request.getAction());

        RefundRecordEntity refund = refundMapper.selectByRefundNo(request.getRefundNo());
        if (refund == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "退款记录不存在");
        }
        if (!"PENDING".equals(refund.getRefundStatus())) {
            throw new BizException(ErrorCodeEnum.STATUS_INVALID, "退款状态不允许处理");
        }

        TradeOrderEntity order = orderMapper.selectByOrderNo(refund.getOrderNo());
        LocalDateTime now = DateTimeUtil.nowUtc();

        if ("APPROVE".equals(request.getAction())) {
            // 调用支付适配层退款
            List<PaymentRecordEntity> payments = paymentMapper.selectByOrderId(order.getId());
            PaymentRecordEntity payment = payments.isEmpty() ? null : payments.get(0);

            RefundRequest refundReq = new RefundRequest();
            refundReq.setRefundNo(refund.getRefundNo());
            refundReq.setOrderNo(order.getOrderNo());
            if (payment != null) {
                refundReq.setThirdTradeNo(payment.getThirdTradeNo());
                refundReq.setPayChannel(payment.getPayChannel());
            }
            refundReq.setRefundAmount(refund.getRefundAmount());
            refundReq.setTotalAmount(order.getPayAmount());
            refundReq.setReason(refund.getRefundReason());

            RefundResponse refundResp = paymentAdapterFactory.refund(refundReq);
            if (refundResp.isSuccess()) {
                refund.setRefundStatus("SUCCESS");
                refund.setThirdRefundNo(refundResp.getThirdRefundNo());
                refund.setCompleteTime(now);
                refund.setOperatorId(operatorId);
                refundMapper.updateById(refund);

                order.setOrderStatus(OrderStatusEnum.REFUNDED.getCode());
                orderMapper.updateById(order);

                auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                        OrderStatusEnum.REFUND_PENDING.getCode(), OrderStatusEnum.REFUNDED.getCode(),
                        "退款成功", null, operatorId);

                log.info("退款成功: refundNo={}, orderNo={}", refund.getRefundNo(), order.getOrderNo());

                // 退款成功后触发逆分账
                try {
                    var settlement = settlementRecordMapper.selectByOrderNo(order.getOrderNo());
                    if (settlement != null && SettlementStatusEnum.SETTLE_SUCCESS.getCode().equals(settlement.getStatus())) {
                        settlementService.reverseSettlement(settlement.getSettleNo(), "退款触发逆分账");
                        log.info("退款触发逆分账成功: refundNo={}, settleNo={}", refund.getRefundNo(), settlement.getSettleNo());
                    }
                } catch (Exception e) {
                    log.error("退款触发逆分账失败: refundNo={}, orderNo={}", refund.getRefundNo(), order.getOrderNo(), e);
                }
            } else {
                refund.setRefundStatus("FAILED");
                refund.setFailReason(refundResp.getErrorMsg());
                refund.setOperatorId(operatorId);
                refundMapper.updateById(refund);
                log.error("退款失败: refundNo={}, error={}", refund.getRefundNo(), refundResp.getErrorMsg());
            }
        } else {
            // 拒绝退款
            refund.setRefundStatus("REJECTED");
            refund.setOperatorId(operatorId);
            refundMapper.updateById(refund);

            // 恢复订单到退款前状态
            String restoreStatus = refund.getPreRefundOrderStatus() != null
                    ? refund.getPreRefundOrderStatus()
                    : OrderStatusEnum.AUTH_GRANTED.getCode();
            order.setOrderStatus(restoreStatus);
            orderMapper.updateById(order);

            auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                    OrderStatusEnum.REFUND_PENDING.getCode(), restoreStatus,
                    "退款被拒绝", null, operatorId);

            log.info("退款已拒绝: refundNo={}, orderNo={}", refund.getRefundNo(), order.getOrderNo());
        }
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
        String status = order.getOrderStatus();
        if (OrderStatusEnum.ORDER_CREATED.getCode().equals(status)) {
            actions.add("PAY");
            actions.add("CANCEL");
        }
        if (OrderStatusEnum.PAY_PENDING_CONFIRM.getCode().equals(status)) {
            actions.add("CANCEL");
        }
        if (OrderStatusEnum.AUTH_GRANTED.getCode().equals(status)
                || OrderStatusEnum.ORDER_COMPLETED.getCode().equals(status)) {
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
