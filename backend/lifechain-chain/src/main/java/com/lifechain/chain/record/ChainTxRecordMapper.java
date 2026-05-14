package com.lifechain.chain.record;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 区块链交易记录 Mapper 接口
 * <p>
 * 基于 MyBatis-Plus {@link BaseMapper} 提供 {@code chain_tx_record} 表的基础 CRUD 操作。
 * 复杂查询通过 {@link com.lifechain.chain.record.ChainTxRecordService} 组合条件构造器实现。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface ChainTxRecordMapper extends BaseMapper<ChainTxRecordEntity> {
}
