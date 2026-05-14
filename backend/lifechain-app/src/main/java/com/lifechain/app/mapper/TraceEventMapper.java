package com.lifechain.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.app.entity.TraceEventEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 追踪事件 Mapper
 * <p>
 * 提供追踪事件的数据访问操作，支持按业务编号查询事件列表。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface TraceEventMapper extends BaseMapper<TraceEventEntity> {

    /**
     * 根据业务类型和业务编号查询追踪事件列表
     *
     * @param bizType 业务类型
     * @param bizNo   业务编号
     * @return 追踪事件列表，按事件时间正序
     */
    @Select("SELECT * FROM trace_event WHERE biz_type = #{bizType} AND biz_no = #{bizNo} " +
            "AND deleted_flag = 0 ORDER BY event_time ASC")
    List<TraceEventEntity> selectByBizNo(@Param("bizType") String bizType,
                                         @Param("bizNo") String bizNo);
}
