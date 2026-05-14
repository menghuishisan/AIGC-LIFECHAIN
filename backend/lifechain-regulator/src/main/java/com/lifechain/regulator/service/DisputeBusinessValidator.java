package com.lifechain.regulator.service;

/**
 * 争议业务校验器
 * <p>
 * SPI接口，由应用层实现以完成跨模块业务关联校验。
 * 用于创建争议时验证订单/作品是否真实存在，以及当事人关系是否合法。
 * </p>
 */
public interface DisputeBusinessValidator {

    /**
     * 校验订单是否存在
     *
     * @param orderNo 订单编号
     * @return true 如果存在
     */
    boolean orderExists(String orderNo);

    /**
     * 校验作品是否存在
     *
     * @param workNo 作品编号
     * @return true 如果存在
     */
    boolean workExists(String workNo);

    /**
     * 校验账户是否为订单的买方或卖方
     *
     * @param orderNo   订单编号
     * @param accountId 账户ID
     * @return true 如果是相关方
     */
    boolean isOrderParty(String orderNo, Long accountId);

    /**
     * 校验账户是否为作品的创作者
     *
     * @param workNo    作品编号
     * @param accountId 账户ID
     * @return true 如果是创作者
     */
    boolean isWorkCreator(String workNo, Long accountId);

    /**
     * 校验作品争议的参与方是否合法。
     * <p>
     * 纯作品争议场景下，至少需要作品创作者作为一方，且申请人与被申请人不能是同一人。
     * </p>
     *
     * @param workNo        作品编号
     * @param applicantId   申请人ID
     * @param respondentId  被申请人ID
     * @return true 如果作品争议参与方合法
     */
    boolean isWorkDisputeParty(String workNo, Long applicantId, Long respondentId);

    /**
     * 校验被申请人是否为订单的对手方
     *
     * @param orderNo      订单编号
     * @param applicantId  申请人ID
     * @param respondentId 被申请人ID
     * @return true 如果被申请人是申请人在该订单中的对手方
     */
    boolean isOrderCounterparty(String orderNo, Long applicantId, Long respondentId);
}
