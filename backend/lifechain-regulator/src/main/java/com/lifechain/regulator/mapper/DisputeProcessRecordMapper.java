package com.lifechain.regulator.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.regulator.entity.DisputeProcessRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 争议处理记录数据访问层
 * <p>
 * 提供争议处理记录表的基础CRUD操作，以及按案件ID查询处理记录的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface DisputeProcessRecordMapper extends BaseMapper<DisputeProcessRecordEntity> {

    /**
     * 根据案件ID查询处理记录列表
     *
     * @param caseId 案件ID
     * @return 处理记录列表（按处理时间升序）
     */
    default List<DisputeProcessRecordEntity> selectByCaseId(Long caseId) {
        return selectList(new LambdaQueryWrapper<DisputeProcessRecordEntity>()
                .eq(DisputeProcessRecordEntity::getCaseId, caseId)
                .orderByAsc(DisputeProcessRecordEntity::getProcessTime));
    }

    /**
     * 根据案件编号查询处理记录列表
     *
     * @param caseNo 案件编号
     * @return 处理记录列表（按处理时间升序）
     */
    default List<DisputeProcessRecordEntity> selectByCaseNo(String caseNo) {
        return selectList(new LambdaQueryWrapper<DisputeProcessRecordEntity>()
                .eq(DisputeProcessRecordEntity::getCaseNo, caseNo)
                .orderByAsc(DisputeProcessRecordEntity::getProcessTime));
    }
}
