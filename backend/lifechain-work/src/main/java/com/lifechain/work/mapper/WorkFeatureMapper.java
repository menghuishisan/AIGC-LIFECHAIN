package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.WorkFeatureEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 作品特征数据访问层
 * <p>
 * 提供作品特征表的基础CRUD操作及按作品ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface WorkFeatureMapper extends BaseMapper<WorkFeatureEntity> {

    /**
     * 根据作品ID查询特征记录（最新一条）
     *
     * @param workId 作品ID
     * @return 特征实体，不存在返回null
     */
    @Select("SELECT * FROM work_feature WHERE work_id = #{workId} AND deleted_flag = 0 ORDER BY created_at DESC LIMIT 1")
    WorkFeatureEntity selectByWorkId(@Param("workId") Long workId);
}
