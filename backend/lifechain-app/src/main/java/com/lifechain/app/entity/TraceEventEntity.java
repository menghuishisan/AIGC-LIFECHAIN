package com.lifechain.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 追踪事件实体
 * <p>
 * 对应 trace_event 表，记录业务实体的生命周期关键事件，
 * 用于全链路追溯和审计。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("trace_event")
public class TraceEventEntity {

    /** 主键（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务类型（WORK/ORDER/SETTLEMENT等） */
    private String bizType;

    /** 业务ID */
    private Long bizId;

    /** 业务编号 */
    private String bizNo;

    /** 事件类型 */
    private String eventType;

    /** 事件描述 */
    private String eventDescription;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人角色 */
    private String operatorRole;

    /** 事件时间(UTC) */
    private LocalDateTime eventTime;

    /** 扩展数据(JSON) */
    private String extraData;

    /** 删除标记 0-未删除 1-已删除 */
    @TableLogic
    private Integer deletedFlag;

    /** 创建时间(UTC) */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间(UTC) */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
