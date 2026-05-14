package com.lifechain.settlement.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.settlement.entity.SettleTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结算模板数据访问层
 * <p>
 * 提供结算模板表的基础CRUD操作，以及按模板编码和状态查询的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface SettleTemplateMapper extends BaseMapper<SettleTemplateEntity> {

    /**
     * 根据模板编码查询
     *
     * @param templateCode 模板编码
     * @return 模板实体，不存在则返回null
     */
    default SettleTemplateEntity selectByTemplateCode(@Param("templateCode") String templateCode) {
        return selectOne(new LambdaQueryWrapper<SettleTemplateEntity>()
                .eq(SettleTemplateEntity::getTemplateCode, templateCode));
    }

    /**
     * 查询所有生效中的模板
     *
     * @return 生效模板列表
     */
    default List<SettleTemplateEntity> selectActiveTemplates() {
        return selectList(new LambdaQueryWrapper<SettleTemplateEntity>()
                .eq(SettleTemplateEntity::getStatus, "ACTIVE")
                .orderByDesc(SettleTemplateEntity::getCreatedAt));
    }
}
