package com.lifechain.auth.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志数据访问层
 * <p>
 * 提供审计日志表的基础CRUD操作。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {

}
