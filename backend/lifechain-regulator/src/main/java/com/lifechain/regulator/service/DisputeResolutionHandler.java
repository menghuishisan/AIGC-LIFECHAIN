package com.lifechain.regulator.service;

/**
 * 争议结论联动处理器
 * <p>
 * SPI接口，由应用层实现以完成跨模块联动操作：退款、逆分账、授权撤销。
 * 仅在争议解决（DISPUTE_RESOLVED，申请方胜诉）时触发。
 * </p>
 */
public interface DisputeResolutionHandler {

    /**
     * 处理争议解决后的联动操作（退款 + 逆分账 + 授权撤销）
     *
     * @param orderNo    关联订单编号
     * @param caseNo     争议案件编号
     * @param operatorId 操作人ID
     */
    void handleDisputeResolved(String orderNo, String caseNo, Long operatorId);
}
