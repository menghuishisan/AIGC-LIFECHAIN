package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.WorkSimilarityCheckEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作品相似度检测数据访问层
 * <p>
 * 提供相似度检测表的基础CRUD操作及按作品ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface WorkSimilarityCheckMapper extends BaseMapper<WorkSimilarityCheckEntity> {

    /**
     * 根据作品ID查询所有相似度检测记录
     *
     * @param workId 待检作品ID
     * @return 相似度检测记录列表
     */
    @Select("SELECT * FROM work_similarity_check WHERE work_id = #{workId} AND deleted_flag = 0 ORDER BY similarity_score DESC")
    List<WorkSimilarityCheckEntity> selectByWorkId(@Param("workId") Long workId);
}
