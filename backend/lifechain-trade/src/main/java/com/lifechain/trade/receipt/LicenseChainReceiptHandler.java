package com.lifechain.trade.receipt;

import com.lifechain.auth.audit.AuditService;
import com.lifechain.chain.receipt.ChainReceiptHandler;
import com.lifechain.chain.record.ChainTxRecordEntity;
import com.lifechain.common.enums.BizTypeEnum;
import com.lifechain.common.enums.ChainStatusEnum;
import com.lifechain.common.enums.LicenseStatusEnum;
import com.lifechain.common.enums.OrderStatusEnum;
import com.lifechain.common.enums.SettlementStatusEnum;
import com.lifechain.common.util.DateTimeUtil;
import com.lifechain.settlement.service.SettlementService;
import com.lifechain.trade.entity.LicenseRecordEntity;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.LicenseRecordMapper;
import com.lifechain.trade.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseChainReceiptHandler implements ChainReceiptHandler {

    private final LicenseRecordMapper licenseRecordMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final AuditService auditService;
    private final SettlementService settlementService;

    @Override
    public String getBizType() {
        return BizTypeEnum.LICENSE.getCode();
    }

    @Override
    public void onChainSuccess(ChainTxRecordEntity record) {
        LicenseRecordEntity license = licenseRecordMapper.selectById(record.getBizId());
        if (license == null) {
            log.warn("授权回执处理失败，未找到授权记录，bizId={}", record.getBizId());
            return;
        }
        String fromStatus = license.getLicenseStatus();
        if (!LicenseStatusEnum.LICENSE_PENDING.getCode().equals(fromStatus)
                || !ChainStatusEnum.CHAIN_SUBMITTED.getCode().equals(license.getChainStatus())) {
            log.info("授权当前状态不需要回执处理，licenseNo={}, status={}", license.getLicenseNo(), fromStatus);
            return;
        }
        license.setLicenseStatus(LicenseStatusEnum.LICENSE_ACTIVE.getCode());
        license.setChainStatus(ChainStatusEnum.CHAIN_SUCCESS.getCode());
        license.setEffectiveTime(DateTimeUtil.nowUtc());
        license.setTxHash(record.getTxHash());
        license.setBlockHeight(record.getBlockHeight());
        licenseRecordMapper.updateById(license);

        // 联动更新订单状态为 AUTH_GRANTED
        if (license.getOrderId() != null) {
            TradeOrderEntity order = tradeOrderMapper.selectById(license.getOrderId());
            if (order != null && OrderStatusEnum.AUTH_GRANTING.getCode().equals(order.getOrderStatus())) {
                String orderFromStatus = order.getOrderStatus();
                order.setOrderStatus(OrderStatusEnum.AUTH_GRANTED.getCode());
                tradeOrderMapper.updateById(order);
                auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                        orderFromStatus, OrderStatusEnum.AUTH_GRANTED.getCode(),
                        "授权链上回执确认，订单授权生效", null, null);

                // 授权生效后自动触发结算
                try {
                    var settlementResult = settlementService.settleOrder(order.getOrderNo(), order.getId(),
                            order.getWorkId(), order.getWorkNo(), order.getPayAmount(), order.getCreatorAccountId());
                    // 仅当结算记录创建成功且未立即失败时，才推进订单到 SETTLEMENT_PENDING
                    if (settlementResult != null
                            && !SettlementStatusEnum.SETTLE_FAILED.getCode().equals(settlementResult.getSettleStatus())) {
                        order.setOrderStatus(OrderStatusEnum.SETTLEMENT_PENDING.getCode());
                        tradeOrderMapper.updateById(order);
                        auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                                OrderStatusEnum.AUTH_GRANTED.getCode(), OrderStatusEnum.SETTLEMENT_PENDING.getCode(),
                                "授权完成，结算已创建", null, null);
                    } else {
                        log.warn("结算创建后状态为失败，订单保持AUTH_GRANTED: orderNo={}", order.getOrderNo());
                    }
                } catch (Exception e) {
                    log.error("授权后触发结算失败，订单保持AUTH_GRANTED: orderNo={}", order.getOrderNo(), e);
                }
            }
        }

        auditService.writeStatusHistory(BizTypeEnum.LICENSE.getCode(), license.getId(), license.getLicenseNo(),
                fromStatus, LicenseStatusEnum.LICENSE_ACTIVE.getCode(),
                "链上回执确认成功", null, null);
        auditService.writeAuditLog(BizTypeEnum.LICENSE.getCode(), license.getId(), license.getLicenseNo(),
                "CHAIN_RECEIPT_SUCCESS", "授权链上回执确认，txHash=" + record.getTxHash(),
                null, "SYSTEM", null, "SUCCESS", null);
        log.info("授权回执处理完成，licenseNo={} -> LICENSE_ACTIVE", license.getLicenseNo());
    }

    @Override
    public void onChainFailed(ChainTxRecordEntity record) {
        LicenseRecordEntity license = licenseRecordMapper.selectById(record.getBizId());
        if (license == null) return;
        String fromStatus = license.getLicenseStatus();
        license.setLicenseStatus(LicenseStatusEnum.LICENSE_PENDING.getCode());
        license.setChainStatus(ChainStatusEnum.CHAIN_FAILED.getCode());
        licenseRecordMapper.updateById(license);

        // 联动回退订单状态到PAY_CONFIRMED（等待重试授权）
        if (license.getOrderId() != null) {
            TradeOrderEntity order = tradeOrderMapper.selectById(license.getOrderId());
            if (order != null && OrderStatusEnum.AUTH_GRANTING.getCode().equals(order.getOrderStatus())) {
                order.setOrderStatus(OrderStatusEnum.ORDER_EXCEPTION.getCode());
                tradeOrderMapper.updateById(order);
                auditService.writeStatusHistory(BizTypeEnum.ORDER.getCode(), order.getId(), order.getOrderNo(),
                        OrderStatusEnum.AUTH_GRANTING.getCode(), OrderStatusEnum.ORDER_EXCEPTION.getCode(),
                        "授权链上回执失败: " + record.getFailReason(), null, null);
            }
        }

        auditService.writeStatusHistory(BizTypeEnum.LICENSE.getCode(), license.getId(), license.getLicenseNo(),
                fromStatus, LicenseStatusEnum.LICENSE_PENDING.getCode(),
                "链上回执失败: " + record.getFailReason(), null, null);
    }
}
