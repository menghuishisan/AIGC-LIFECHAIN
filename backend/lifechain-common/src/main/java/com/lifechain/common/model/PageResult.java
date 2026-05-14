package com.lifechain.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应结构
 * <p>
 * 所有列表接口统一返回此分页对象。
 * </p>
 *
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> implements Serializable {

    /** 数据记录列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码（从1开始） */
    private int pageNo;

    /** 每页大小 */
    private int pageSize;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, int pageNo, int pageSize) {
        this.records = records;
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

    /**
     * 构建分页结果
     *
     * @param records  数据列表
     * @param total    总记录数
     * @param pageNo   当前页码
     * @param pageSize 每页大小
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNo, int pageSize) {
        return new PageResult<>(records, total, pageNo, pageSize);
    }
}
