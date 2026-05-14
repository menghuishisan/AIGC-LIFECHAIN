package com.lifechain.app.service;

import com.lifechain.regulator.service.DisputeResolutionHandler;
import com.lifechain.settlement.dto.SettlementRecordVO;
import com.lifechain.settlement.service.SettlementService;
import com.lifechain.trade.dto.RefundApplyRequest;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.trade.service.LicenseService;
import com.lifechain.trade.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 争议结论联动处理器实现
 * <p>
 * 在应用层实现跨模块联动：当争议解决（申请方胜诉）时，依次执行退款、逆分账、授权撤销。
 * 每步操作独立容错，单步失败不影响后续步骤执行。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeResolutionHandlerImpl implements DisputeResolutionHandler {

    private final OrderService orderService;
    private final SettlementService settlementService;
    private final LicenseService licenseService;
    private final TradeOrderMapper tradeOrderMapper;

    @Override
    public void handleDisputeResolved(String orderNo, String caseNo, Long operatorId) {
        log.info("争议结论联动处理开始，caseNo={}, orderNo={}", caseNo, orderNo);

        // 查询订单以获取买方ID
        TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("争议联动：订单不存在，跳过，orderNo={}", orderNo);
            return;
        }

        // 1. 触发退款申请
        try {
            RefundApplyRequest refundReq = new RefundApplyRequest();
            refundReq.setOrderNo(orderNo);
            refundReq.setReason("争议案件 " + caseNo + " 结案：申请方胜诉，系统自动退款");
            refundReq.setRequestId(UUID.randomUUID().toString());
            orderService.applyRefund(order.getBuyerAccountId(), refundReq);
            log.info("争议联动退款申请成功，caseNo={}, orderNo={}", caseNo, orderNo);
        } catch (Exception e) {
            log.warn("争议联动退款申请失败，caseNo={}, orderNo={}", caseNo, orderNo, e);
        }

        // 2. 触发逆分账
        try {
            SettlementRecordVO settlement = settlementService.getSettlementByOrderNo(orderNo);
            if (settlement != null && settlement.getSettleNo() != null) {
                settlementService.reverseSettlement(settlement.getSettleNo(),
                        "争议案件 " + caseNo + " 结案：申请方胜诉，系统自动逆分账");
                log.info("争议联动逆分账成功，caseNo={}, settleNo={}", caseNo, settlement.getSettleNo());
            } else {
                log.info("争议联动：无结算记录，跳过逆分账，orderNo={}", orderNo);
            }
        } catch (Exception e) {
            log.warn("争议联动逆分账失败，caseNo={}, orderNo={}", caseNo, orderNo, e);
        }

        // 3. 触发授权撤销
        try {
            licenseService.revokeLicenseByOrderNo(orderNo,
                    "争议案件 " + caseNo + " 结案：申请方胜诉，系统自动撤销授权");
            log.info("争议联动授权撤销成功，caseNo={}, orderNo={}", caseNo, orderNo);
        } catch (Exception e) {
            log.warn("争议联动授权撤销失败，caseNo={}, orderNo={}", caseNo, orderNo, e);
        }

        log.info("争议结论联动处理完成，caseNo={}, orderNo={}", caseNo, orderNo);
    }
}
