package com.lifechain.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体基类
 * <p>
 * 所有数据库实体继承此类，统一主键策略、软删除标识和时间字段。
 * 主键使用雪花ID（bigint），软删除使用 deleted_flag（0未删除/1已删除）。
 * </p>
 */
@Data
public abstract class BaseEntity implements Serializable {

    /** 主键ID（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 软删除标识：0-未删除，1-已删除 */
    @TableLogic
    @TableField("deleted_flag")
    private Integer deletedFlag;

    /** 创建时间（UTC） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
