package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.trade.entity.WorkListingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 作品上架数据访问层
 * <p>
 * 提供作品上架表的基础CRUD操作，以及常用的按编号、作品ID和状态查询方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface WorkListingMapper extends BaseMapper<WorkListingEntity> {

    /**
     * 根据上架编号查询
     *
     * @param listingNo 上架编号
     * @return 上架记录实体，不存在则返回null
     */
    default WorkListingEntity selectByListingNo(@Param("listingNo") String listingNo) {
        return selectOne(new LambdaQueryWrapper<WorkListingEntity>()
                .eq(WorkListingEntity::getListingNo, listingNo));
    }

    /**
     * 根据作品ID查询上架记录列表
     *
     * @param workId 作品ID
     * @return 该作品的所有上架记录
     */
    default List<WorkListingEntity> selectByWorkId(@Param("workId") Long workId) {
        return selectList(new LambdaQueryWrapper<WorkListingEntity>()
                .eq(WorkListingEntity::getWorkId, workId)
                .orderByDesc(WorkListingEntity::getCreatedAt));
    }

    /**
     * 根据作品编号查询上架记录列表
     *
     * @param workNo 作品编号
     * @return 该作品的所有上架记录
     */
    default List<WorkListingEntity> selectByWorkNo(@Param("workNo") String workNo) {
        return selectList(new LambdaQueryWrapper<WorkListingEntity>()
                .eq(WorkListingEntity::getWorkNo, workNo)
                .orderByDesc(WorkListingEntity::getCreatedAt));
    }

    /**
     * 按状态分页查询上架记录
     *
     * @param page   分页参数
     * @param status 上架状态
     * @return 分页结果
     */
    default IPage<WorkListingEntity> selectPageByStatus(Page<WorkListingEntity> page,
                                                        @Param("status") String status) {
        LambdaQueryWrapper<WorkListingEntity> wrapper = new LambdaQueryWrapper<WorkListingEntity>()
                .eq(WorkListingEntity::getStatus, status)
                .orderByDesc(WorkListingEntity::getListTime);
        return selectPage(page, wrapper);
    }
}
