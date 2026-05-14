package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.ClaimReviewRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 确权审核记录数据访问层
 * <p>
 * 提供确权审核记录表的基础CRUD操作及按确权申请ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface ClaimReviewRecordMapper extends BaseMapper<ClaimReviewRecordEntity> {

    /**
     * 根据确权申请ID查询审核记录列表
     *
     * @param claimId 确权申请ID
     * @return 审核记录列表
     */
    @Select("SELECT * FROM claim_review_record WHERE claim_id = #{claimId} AND deleted_flag = 0 ORDER BY review_time ASC")
    List<ClaimReviewRecordEntity> selectByClaimId(@Param("claimId") Long claimId);
}
