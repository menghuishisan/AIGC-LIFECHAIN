package com.lifechain.regulator.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.regulator.entity.DisputeCaseEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 争议案件数据访问层
 * <p>
 * 提供争议案件表的基础CRUD操作，以及按案件编号、订单编号查询的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface DisputeCaseMapper extends BaseMapper<DisputeCaseEntity> {

    /**
     * 根据案件编号查询争议案件
     *
     * @param caseNo 案件编号
     * @return 争议案件实体，不存在则返回null
     */
    default DisputeCaseEntity selectByCaseNo(String caseNo) {
        return selectOne(new LambdaQueryWrapper<DisputeCaseEntity>()
                .eq(DisputeCaseEntity::getCaseNo, caseNo));
    }

    /**
     * 根据订单编号查询争议案件列表
     *
     * @param orderNo 订单编号
     * @return 争议案件列表
     */
    default List<DisputeCaseEntity> selectByOrderNo(String orderNo) {
        return selectList(new LambdaQueryWrapper<DisputeCaseEntity>()
                .eq(DisputeCaseEntity::getOrderNo, orderNo)
                .orderByDesc(DisputeCaseEntity::getCreatedAt));
    }

    /**
     * 根据申请人账户ID查询争议案件列表
     *
     * @param applicantAccountId 申请人账户ID
     * @return 争议案件列表
     */
    default List<DisputeCaseEntity> selectByApplicantAccountId(Long applicantAccountId) {
        return selectList(new LambdaQueryWrapper<DisputeCaseEntity>()
                .eq(DisputeCaseEntity::getApplicantAccountId, applicantAccountId)
                .orderByDesc(DisputeCaseEntity::getCreatedAt));
    }

    /**
     * 根据被申请人账户ID查询争议案件列表
     *
     * @param respondentAccountId 被申请人账户ID
     * @return 争议案件列表
     */
    default List<DisputeCaseEntity> selectByRespondentAccountId(Long respondentAccountId) {
        return selectList(new LambdaQueryWrapper<DisputeCaseEntity>()
                .eq(DisputeCaseEntity::getRespondentAccountId, respondentAccountId)
                .orderByDesc(DisputeCaseEntity::getCreatedAt));
    }
}
