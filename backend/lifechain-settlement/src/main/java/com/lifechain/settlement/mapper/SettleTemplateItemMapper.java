package com.lifechain.settlement.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.settlement.entity.SettleTemplateItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结算模板明细数据访问层
 * <p>
 * 提供结算模板明细表的基础CRUD操作，以及按模板ID查询明细列表的方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface SettleTemplateItemMapper extends BaseMapper<SettleTemplateItemEntity> {

    /**
     * 根据模板ID查询所有明细
     *
     * @param templateId 结算模板ID
     * @return 模板明细列表
     */
    default List<SettleTemplateItemEntity> selectByTemplateId(@Param("templateId") Long templateId) {
        return selectList(new LambdaQueryWrapper<SettleTemplateItemEntity>()
                .eq(SettleTemplateItemEntity::getTemplateId, templateId)
                .orderByAsc(SettleTemplateItemEntity::getRoleType));
    }
}
