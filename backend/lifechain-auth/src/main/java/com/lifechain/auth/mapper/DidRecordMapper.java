package com.lifechain.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.auth.entity.DidRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * DID记录数据访问层
 * <p>
 * 提供DID记录表的基础CRUD操作及按账户ID、DID编号查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface DidRecordMapper extends BaseMapper<DidRecordEntity> {

    /**
     * 根据账户ID查询DID记录（最新一条有效记录）
     *
     * @param accountId 账户ID
     * @return DID记录实体，不存在返回null
     */
    @Select("SELECT * FROM did_record WHERE account_id = #{accountId} AND deleted_flag = 0 ORDER BY created_at DESC LIMIT 1")
    DidRecordEntity selectByAccountId(@Param("accountId") Long accountId);

    /**
     * 根据DID编号查询DID记录
     *
     * @param didNo DID编号
     * @return DID记录实体，不存在返回null
     */
    @Select("SELECT * FROM did_record WHERE did_no = #{didNo} AND deleted_flag = 0")
    DidRecordEntity selectByDidNo(@Param("didNo") String didNo);
}
