package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 监管员争议列表视图对象
 * <p>
 * 用于监管后台争议案件列表展示，包含案件摘要信息。
 * 不加载完整证据文件列表和完整处理记录，避免列表过重。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class RegulatorDisputeListVO implements Serializable {

    /** 案件编号 */
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

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
