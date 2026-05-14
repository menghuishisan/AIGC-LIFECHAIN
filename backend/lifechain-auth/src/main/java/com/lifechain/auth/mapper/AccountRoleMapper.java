package com.lifechain.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.auth.entity.AccountRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 账户角色数据访问层
 * <p>
 * 提供账户角色表的基础CRUD操作及按账户ID查询角色列表的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface AccountRoleMapper extends BaseMapper<AccountRoleEntity> {

    /**
     * 根据账户ID查询所有角色（仅ACTIVE状态）
     *
     * @param accountId 账户ID
     * @return 角色列表
     */
    @Select("SELECT * FROM account_role WHERE account_id = #{accountId} AND status = 'ACTIVE' AND deleted_flag = 0")
    List<AccountRoleEntity> selectByAccountId(@Param("accountId") Long accountId);
}
