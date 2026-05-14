package com.lifechain.app.service;

import com.lifechain.regulator.service.DisputeBusinessValidator;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.trade.mapper.TradeOrderMapper;
import com.lifechain.work.entity.WorkEntity;
import com.lifechain.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 争议业务校验器实现
 * <p>
 * 在应用层实现跨模块业务关联校验，验证争议创建时的订单/作品存在性和当事人关系。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DisputeBusinessValidatorImpl implements DisputeBusinessValidator {

    private final TradeOrderMapper tradeOrderMapper;
    private final WorkMapper workMapper;

    @Override
    public boolean orderExists(String orderNo) {
        return tradeOrderMapper.selectByOrderNo(orderNo) != null;
    }

    @Override
    public boolean workExists(String workNo) {
        return workMapper.selectByWorkNo(workNo) != null;
    }

    @Override
    public boolean isOrderParty(String orderNo, Long accountId) {
        TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return false;
        }
        return accountId.equals(order.getBuyerAccountId())
                || accountId.equals(order.getCreatorAccountId());
    }

    @Override
    public boolean isWorkCreator(String workNo, Long accountId) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null) {
            return false;
        }
        return accountId.equals(work.getCreatorAccountId());
    }

    @Override
    public boolean isWorkDisputeParty(String workNo, Long applicantId, Long respondentId) {
        WorkEntity work = workMapper.selectByWorkNo(workNo);
        if (work == null || applicantId == null || respondentId == null) {
            return false;
        }
        if (applicantId.equals(respondentId)) {
            return false;
        }
        Long creatorAccountId = work.getCreatorAccountId();
        return creatorAccountId != null
                && (creatorAccountId.equals(applicantId) || creatorAccountId.equals(respondentId));
    }

    @Override
    public boolean isOrderCounterparty(String orderNo, Long applicantId, Long respondentId) {
        TradeOrderEntity order = tradeOrderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            return false;
        }
        // 申请人是买方 -> 被申请人应该是卖方，反之亦然
        if (applicantId.equals(order.getBuyerAccountId())) {
            return respondentId.equals(order.getCreatorAccountId());
        }
        if (applicantId.equals(order.getCreatorAccountId())) {
            return respondentId.equals(order.getBuyerAccountId());
        }
        return false;
    }
}
