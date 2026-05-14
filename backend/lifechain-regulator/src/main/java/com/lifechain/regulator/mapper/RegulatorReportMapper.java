package com.lifechain.regulator.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.regulator.entity.RegulatorReportEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 监管报告数据访问层
 * <p>
 * 提供监管报告表的基础CRUD操作，以及按报告编号查询和待完成报告查询的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface RegulatorReportMapper extends BaseMapper<RegulatorReportEntity> {

    /**
     * 根据报告编号查询监管报告
     *
     * @param reportNo 报告编号
     * @return 监管报告实体，不存在则返回null
     */
    default RegulatorReportEntity selectByReportNo(String reportNo) {
        return selectOne(new LambdaQueryWrapper<RegulatorReportEntity>()
                .eq(RegulatorReportEntity::getReportNo, reportNo));
    }

    /**
     * 查询待完成的监管报告（状态为DRAFT或GENERATING）
     *
     * @return 待完成监管报告列表
     */
    default List<RegulatorReportEntity> selectPendingReports() {
        return selectList(new LambdaQueryWrapper<RegulatorReportEntity>()
                .in(RegulatorReportEntity::getStatus, "DRAFT", "GENERATING")
                .orderByDesc(RegulatorReportEntity::getCreatedAt));
    }

    /**
     * 根据报告类型查询报告列表
     *
     * @param reportType 报告类型
     * @return 监管报告列表
     */
    default List<RegulatorReportEntity> selectByReportType(String reportType) {
        return selectList(new LambdaQueryWrapper<RegulatorReportEntity>()
                .eq(RegulatorReportEntity::getReportType, reportType)
                .orderByDesc(RegulatorReportEntity::getCreatedAt));
    }
}
