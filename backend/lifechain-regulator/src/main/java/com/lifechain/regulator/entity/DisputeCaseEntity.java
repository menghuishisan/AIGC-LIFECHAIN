package com.lifechain.regulator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 争议案件实体
 * <p>
 * 对应数据库表 {@code dispute_case}，记录争议案件的完整生命周期信息，
 * 包含申请人/被申请人、争议类型、处理状态、结论结果及链上存证等核心字段。
 * 争议生命周期：提交 → 受理 → 待补证据 → 审查 → 已解决/已驳回/已关闭。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispute_case")
public class DisputeCaseEntity extends BaseEntity {

    /** 争议案件编号（对外唯一标识） */
    @TableField("case_no")
    private String caseNo;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 申请人账户ID */
    @TableField("applicant_account_id")
    private Long applicantAccountId;

    /** 被申请人账户ID */
    @TableField("respondent_account_id")
    private Long respondentAccountId;

    /** 争议类型 */
    @TableField("dispute_type")
    private String disputeType;

    /** 争议状态 */
    @TableField("status")
    private String status;

    /** 争议描述 */
    @TableField("description")
    private String description;

    /** 提交时间（UTC） */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /** 受理时间（UTC） */
    @TableField("accept_time")
    private LocalDateTime acceptTime;

    /** 结案时间（UTC） */
    @TableField("close_time")
    private LocalDateTime closeTime;

    /** 结果摘要 */
    @TableField("result_summary")
    private String resultSummary;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 链上状态 */
    @TableField("chain_status")
    private String chainStatus;

    /** 交易哈希 */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;
}
