package com.lifechain.auth.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 状态变更历史数据访问层
 * <p>
 * 提供状态变更历史表的基础CRUD操作。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface StatusHistoryMapper extends BaseMapper<StatusHistoryEntity> {

}
