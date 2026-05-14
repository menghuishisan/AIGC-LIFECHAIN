package com.lifechain.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.auth.entity.SubjectProfileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 主体信息数据访问层
 * <p>
 * 提供主体信息表的基础CRUD操作及按账户ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface SubjectProfileMapper extends BaseMapper<SubjectProfileEntity> {

    /**
     * 根据账户ID查询主体信息
     *
     * @param accountId 账户ID
     * @return 主体信息实体，不存在返回null
     */
    @Select("SELECT * FROM subject_profile WHERE account_id = #{accountId} AND deleted_flag = 0")
    SubjectProfileEntity selectByAccountId(@Param("accountId") Long accountId);
}
