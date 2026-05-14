package com.lifechain.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一分页查询参数
 */
@Data
public class PageQuery implements Serializable {

    /** 页码（从1开始） */
    @Min(value = 1, message = "页码最小为1")
    private int pageNo = 1;

    /** 每页大小 */
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = 200, message = "每页大小最大为200")
    private int pageSize = 20;
}
