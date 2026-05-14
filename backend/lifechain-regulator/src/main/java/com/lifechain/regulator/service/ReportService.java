package com.lifechain.regulator.service;

import com.lifechain.common.model.PageQuery;
import com.lifechain.common.model.PageResult;
import com.lifechain.regulator.dto.CreateReportRequest;
import com.lifechain.regulator.dto.HandleReportRequest;
import com.lifechain.regulator.dto.ReportVO;

/**
 * 监管报告服务接口
 * <p>
 * 提供监管报告的创建、处理（生成/完成/失败）和查询操作。
 * 已完成的报告通过摘要哈希上链存证，确保报告的不可篡改性。
 * 所有状态变更均记录审计日志和状态变更历史。
 * </p>
 *
 * @author LifeChain
 */
public interface ReportService {

    /**
     * 创建监管报告
     * <p>
     * 生成报告编号，初始状态为DRAFT，记录创建人和创建时间。
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    创建请求
     * @return 监管报告视图对象
     */
    ReportVO createReport(Long operatorId, CreateReportRequest request);

    /**
     * 处理监管报告
     * <p>
     * 支持的处理动作：
     * <ul>
     *   <li>GENERATE: 触发生成 → GENERATING</li>
     *   <li>COMPLETE: 标记完成 → COMPLETED（同时上链存证）</li>
     *   <li>FAIL: 标记失败 → FAILED</li>
     * </ul>
     * </p>
     *
     * @param operatorId 操作人ID
     * @param request    处理请求
     * @return 更新后的监管报告视图对象
     */
    ReportVO handleReport(Long operatorId, HandleReportRequest request);

    /**
     * 分页查询监管报告列表
     *
     * @param reportType 报告类型（可选）
     * @param status     状态（可选）
     * @param query      分页参数
     * @return 分页监管报告列表
     */
    PageResult<ReportVO> listReports(String reportType, String status, PageQuery query);

    /**
     * 根据报告编号查询监管报告详情
     *
     * @param reportNo 报告编号
     * @return 监管报告视图对象
     */
    ReportVO getByReportNo(String reportNo);
}
