package com.lifechain.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.auth.entity.SubjectAuthRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 主体认证记录数据访问层
 * <p>
 * 提供主体认证记录表的基础CRUD操作。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface SubjectAuthRecordMapper extends BaseMapper<SubjectAuthRecordEntity> {

}
