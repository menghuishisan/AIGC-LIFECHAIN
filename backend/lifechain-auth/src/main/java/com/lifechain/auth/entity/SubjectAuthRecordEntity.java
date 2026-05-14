package com.lifechain.auth.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 主体认证记录实体
 * <p>
 * 对应数据库表 {@code subject_auth_record}，记录每一次实名认证的提交和审核过程，
 * 包括审核动作、审核状态、审核人信息、审核意见等。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("subject_auth_record")
public class SubjectAuthRecordEntity extends BaseEntity {

    /** 关联主体ID */
    @TableField("subject_id")
    private Long subjectId;

    /** 认证动作 */
    @TableField("auth_action")
    private String authAction;

    /** 认证状态 */
    @TableField("auth_status")
    private String authStatus;

    /** 审核人ID */
    @TableField("reviewer_id")
    private Long reviewerId;

    /** 审核意见 */
    @TableField("review_comment")
    private String reviewComment;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 提交时间（UTC） */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /** 审核时间（UTC） */
    @TableField("review_time")
    private LocalDateTime reviewTime;
}
