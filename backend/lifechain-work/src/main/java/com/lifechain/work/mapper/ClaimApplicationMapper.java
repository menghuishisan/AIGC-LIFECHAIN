package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.ClaimApplicationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 确权申请数据访问层
 * <p>
 * 提供确权申请表的基础CRUD操作及按确权编号、作品ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface ClaimApplicationMapper extends BaseMapper<ClaimApplicationEntity> {

    /**
     * 根据确权编号查询确权申请
     *
     * @param claimNo 确权编号
     * @return 确权申请实体，不存在返回null
     */
    @Select("SELECT * FROM claim_application WHERE claim_no = #{claimNo} AND deleted_flag = 0")
    ClaimApplicationEntity selectByClaimNo(@Param("claimNo") String claimNo);

    /**
     * 根据作品ID查询确权申请列表
     *
     * @param workId 作品ID
     * @return 确权申请列表
     */
    @Select("SELECT * FROM claim_application WHERE work_id = #{workId} AND deleted_flag = 0 ORDER BY created_at DESC")
    List<ClaimApplicationEntity> selectByWorkId(@Param("workId") Long workId);
}
