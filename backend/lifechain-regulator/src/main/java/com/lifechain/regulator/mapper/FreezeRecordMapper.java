package com.lifechain.regulator.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.regulator.entity.FreezeRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 冻结记录数据访问层
 * <p>
 * 提供冻结记录表的基础CRUD操作，以及按冻结编号、目标编号和生效冻结查询的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface FreezeRecordMapper extends BaseMapper<FreezeRecordEntity> {

    /**
     * 根据冻结编号查询冻结记录
     *
     * @param freezeNo 冻结编号
     * @return 冻结记录实体，不存在则返回null
     */
    default FreezeRecordEntity selectByFreezeNo(String freezeNo) {
        return selectOne(new LambdaQueryWrapper<FreezeRecordEntity>()
                .eq(FreezeRecordEntity::getFreezeNo, freezeNo));
    }

    /**
     * 根据目标类型和目标编号查询冻结记录
     *
     * @param targetType 目标类型
     * @param targetNo   目标编号
     * @return 冻结记录列表
     */
    default List<FreezeRecordEntity> selectByTargetNo(String targetType, String targetNo) {
        return selectList(new LambdaQueryWrapper<FreezeRecordEntity>()
                .eq(FreezeRecordEntity::getTargetType, targetType)
                .eq(FreezeRecordEntity::getTargetNo, targetNo)
                .orderByDesc(FreezeRecordEntity::getCreatedAt));
    }

    /**
     * 查询目标的生效冻结记录（状态为已发起或已批准）
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 生效冻结记录列表
     */
    default List<FreezeRecordEntity> selectActiveFreezeByTarget(String targetType, Long targetId) {
        return selectList(new LambdaQueryWrapper<FreezeRecordEntity>()
                .eq(FreezeRecordEntity::getTargetType, targetType)
                .eq(FreezeRecordEntity::getTargetId, targetId)
                .in(FreezeRecordEntity::getFreezeStatus,
                        "FREEZE_APPLIED",
                        "FREEZE_APPROVED",
                        "FREEZE_APPROVED_PENDING_CHAIN",
                        "UNFREEZE_PENDING_CHAIN")
                .orderByDesc(FreezeRecordEntity::getCreatedAt));
    }

    /**
     * 查询目标编号的生效冻结记录
     *
     * @param targetType 目标类型
     * @param targetNo   目标编号
     * @return 生效冻结记录列表
     */
    default List<FreezeRecordEntity> selectActiveFreezeByTargetNo(String targetType, String targetNo) {
        return selectList(new LambdaQueryWrapper<FreezeRecordEntity>()
                .eq(FreezeRecordEntity::getTargetType, targetType)
                .eq(FreezeRecordEntity::getTargetNo, targetNo)
                .in(FreezeRecordEntity::getFreezeStatus,
                        "FREEZE_APPLIED",
                        "FREEZE_APPROVED",
                        "FREEZE_APPROVED_PENDING_CHAIN",
                        "UNFREEZE_PENDING_CHAIN")
                .orderByDesc(FreezeRecordEntity::getCreatedAt));
    }

    /**
     * 根据申请人ID查询冻结记录
     *
     * @param applyUserId 申请人ID
     * @return 冻结记录列表
     */
    default List<FreezeRecordEntity> selectByApplyUserId(Long applyUserId) {
        return selectList(new LambdaQueryWrapper<FreezeRecordEntity>()
                .eq(FreezeRecordEntity::getApplyUserId, applyUserId)
                .orderByDesc(FreezeRecordEntity::getCreatedAt));
    }
}
