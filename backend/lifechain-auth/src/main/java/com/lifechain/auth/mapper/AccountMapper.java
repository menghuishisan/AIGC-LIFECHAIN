package com.lifechain.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.auth.entity.AccountEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 账户数据访问层
 * <p>
 * 提供账户表的基础CRUD操作及按手机号、账户编号查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface AccountMapper extends BaseMapper<AccountEntity> {

    /**
     * 根据手机号查询账户
     *
     * @param mobile 手机号
     * @return 账户实体，不存在返回null
     */
    @Select("SELECT * FROM account WHERE mobile = #{mobile} AND deleted_flag = 0")
    AccountEntity selectByMobile(@Param("mobile") String mobile);

    /**
     * 根据账户编号查询账户
     *
     * @param accountNo 账户编号
     * @return 账户实体，不存在返回null
     */
    @Select("SELECT * FROM account WHERE account_no = #{accountNo} AND deleted_flag = 0")
    AccountEntity selectByAccountNo(@Param("accountNo") String accountNo);
}
