package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 争议处理记录视图对象
 * <p>
 * 展示争议案件处理流程中的单次操作记录，包含操作人、动作和处理意见。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ProcessRecordVO implements Serializable {

    /** 操作方角色 */
    private String operatorRole;

    /** 处理动作 */
    private String action;

    /** 处理结果 */
    private String actionResult;

    /** 处理意见 */
    private String comment;

    /** 处理时间 */
    private LocalDateTime processTime;
}
