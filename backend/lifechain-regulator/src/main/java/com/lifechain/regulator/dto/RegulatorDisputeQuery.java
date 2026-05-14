package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 监管员争议查询条件
 * <p>
 * 用于监管员后台全量争议案件列表的筛选条件，不按当事人归属过滤。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class RegulatorDisputeQuery implements Serializable {

    /** 案件编号（模糊匹配） */
    private String caseNo;

    /** 争议状态 */
    private String status;

    /** 争议类型 */
    private String disputeType;

    /** 关联订单编号 */
    private String orderNo;

    /** 关联作品编号 */
    private String workNo;

    /** 申请人账户编号 */
    private String applicantAccountNo;

    /** 被申请人账户编号 */
    private String respondentAccountNo;

    /** 查询开始时间 */
    private LocalDateTime dateFrom;

    /** 查询结束时间 */
    private LocalDateTime dateTo;
}
