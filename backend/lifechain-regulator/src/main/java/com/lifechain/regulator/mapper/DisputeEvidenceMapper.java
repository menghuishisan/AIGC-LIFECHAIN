package com.lifechain.regulator.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.regulator.entity.DisputeEvidenceEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 争议证据数据访问层
 * <p>
 * 提供争议证据表的基础CRUD操作，以及按案件ID查询证据列表的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface DisputeEvidenceMapper extends BaseMapper<DisputeEvidenceEntity> {

    /**
     * 根据案件ID查询证据列表
     *
     * @param caseId 案件ID
     * @return 证据列表
     */
    default List<DisputeEvidenceEntity> selectByCaseId(Long caseId) {
        return selectList(new LambdaQueryWrapper<DisputeEvidenceEntity>()
                .eq(DisputeEvidenceEntity::getCaseId, caseId)
                .orderByAsc(DisputeEvidenceEntity::getCreatedAt));
    }

    /**
     * 根据案件编号查询证据列表
     *
     * @param caseNo 案件编号
     * @return 证据列表
     */
    default List<DisputeEvidenceEntity> selectByCaseNo(String caseNo) {
        return selectList(new LambdaQueryWrapper<DisputeEvidenceEntity>()
                .eq(DisputeEvidenceEntity::getCaseNo, caseNo)
                .orderByAsc(DisputeEvidenceEntity::getCreatedAt));
    }
}
