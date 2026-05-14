package com.lifechain.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.work.entity.VerifyQueryLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 验证查询日志数据访问层
 * <p>
 * 提供验证查询日志表的基础CRUD操作。
 * 查询日志以写入为主，查询通过MyBatis-Plus条件构造器实现。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface VerifyQueryLogMapper extends BaseMapper<VerifyQueryLogEntity> {
}
