package com.lifechain.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 追踪事件视图对象
 * <p>
 * 用于接口返回的追踪事件信息，包含事件类型、时间、描述、操作人和扩展数据。
 * </p>
 *
 * @author LifeChain
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceEventVO {

    /** 事件类型 */
    private String eventType;

    /** 事件时间 */
    private LocalDateTime eventTime;

    /** 事件描述 */
    private String description;

    /** 操作人角色 */
    private String operator;

    /** 扩展数据(JSON) */
    private String extraData;
}
