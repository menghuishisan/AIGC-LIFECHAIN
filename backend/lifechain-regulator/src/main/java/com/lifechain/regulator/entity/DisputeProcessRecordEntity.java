package com.lifechain.regulator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 争议处理记录实体
 * <p>
 * 对应数据库表 {@code dispute_process_record}，记录争议案件处理过程中的每一步操作，
 * 包含操作人、处理动作、处理结果、处理意见和处理时间等核心字段。
 * 用于完整追踪争议案件的处理流程。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispute_process_record")
public class DisputeProcessRecordEntity extends BaseEntity {

    /** 争议案件ID */
    @TableField("case_id")
    private Long caseId;

    /** 争议案件编号 */
    @TableField("case_no")
    private String caseNo;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 处理动作 */
    @TableField("action")
    private String action;

    /** 处理结果 */
    @TableField("action_result")
    private String actionResult;

    /** 处理意见 */
    @TableField("comment")
    private String comment;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 处理时间（UTC） */
    @TableField("process_time")
    private LocalDateTime processTime;
}
