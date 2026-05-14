package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.WorkFileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作品文件数据访问层
 * <p>
 * 提供作品文件表的基础CRUD操作及按作品ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface WorkFileMapper extends BaseMapper<WorkFileEntity> {

    /**
     * 根据作品ID查询所有文件记录
     *
     * @param workId 作品ID
     * @return 文件记录列表
     */
    @Select("SELECT * FROM work_file WHERE work_id = #{workId} AND deleted_flag = 0 ORDER BY created_at ASC")
    List<WorkFileEntity> selectByWorkId(@Param("workId") Long workId);
}
