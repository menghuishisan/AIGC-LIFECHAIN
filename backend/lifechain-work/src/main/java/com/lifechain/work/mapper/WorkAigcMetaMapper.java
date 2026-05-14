package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.WorkAigcMetaEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 作品AIGC元数据数据访问层
 * <p>
 * 提供AIGC元数据表的基础CRUD操作及按作品ID查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface WorkAigcMetaMapper extends BaseMapper<WorkAigcMetaEntity> {

    /**
     * 根据作品ID查询AIGC元数据
     *
     * @param workId 作品ID
     * @return AIGC元数据实体，不存在返回null
     */
    @Select("SELECT * FROM work_aigc_meta WHERE work_id = #{workId} AND deleted_flag = 0")
    WorkAigcMetaEntity selectByWorkId(@Param("workId") Long workId);
}
