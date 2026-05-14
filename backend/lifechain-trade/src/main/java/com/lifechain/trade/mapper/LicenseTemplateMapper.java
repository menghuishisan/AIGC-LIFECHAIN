package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.trade.entity.LicenseTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 授权模板数据访问层
 * <p>
 * 提供授权模板表的基础CRUD操作及根据模板编码查询的自定义方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface LicenseTemplateMapper extends BaseMapper<LicenseTemplateEntity> {

    /**
     * 根据模板编码查询授权模板
     *
     * @param templateCode 模板编码
     * @return 授权模板实体，不存在则返回null
     */
    default LicenseTemplateEntity selectByTemplateCode(@Param("templateCode") String templateCode) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LicenseTemplateEntity>()
                .eq(LicenseTemplateEntity::getTemplateCode, templateCode));
    }
}
