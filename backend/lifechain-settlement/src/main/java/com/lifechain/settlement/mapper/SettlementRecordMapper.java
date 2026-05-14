package com.lifechain.settlement.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.settlement.entity.SettlementRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 结算记录数据访问层
 * <p>
 * 提供结算记录表的基础CRUD操作，以及按结算编号、订单ID、订单编号和状态查询的扩展方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface SettlementRecordMapper extends BaseMapper<SettlementRecordEntity> {

    /**
     * 根据结算编号查询
     *
     * @param settleNo 结算编号
     * @return 结算记录实体，不存在则返回null
     */
    default SettlementRecordEntity selectBySettleNo(@Param("settleNo") String settleNo) {
        return selectOne(new LambdaQueryWrapper<SettlementRecordEntity>()
                .eq(SettlementRecordEntity::getSettleNo, settleNo));
    }

    /**
     * 根据订单ID查询结算记录
     *
     * @param orderId 订单ID
     * @return 结算记录实体，不存在则返回null
     */
    default SettlementRecordEntity selectByOrderId(@Param("orderId") Long orderId) {
        return selectOne(new LambdaQueryWrapper<SettlementRecordEntity>()
                .eq(SettlementRecordEntity::getOrderId, orderId)
                .orderByDesc(SettlementRecordEntity::getCreatedAt)
                .last("LIMIT 1"));
    }

    /**
     * 根据订单编号查询结算记录
     *
     * @param orderNo 订单编号
     * @return 结算记录实体，不存在则返回null
     */
    default SettlementRecordEntity selectByOrderNo(@Param("orderNo") String orderNo) {
        return selectOne(new LambdaQueryWrapper<SettlementRecordEntity>()
                .eq(SettlementRecordEntity::getOrderNo, orderNo)
                .orderByDesc(SettlementRecordEntity::getCreatedAt)
                .last("LIMIT 1"));
    }

    /**
     * 查询结算失败待重试的记录
     *
     * @return 待重试结算记录列表
     */
    default List<SettlementRecordEntity> selectFailedRecords() {
        return selectList(new LambdaQueryWrapper<SettlementRecordEntity>()
                .eq(SettlementRecordEntity::getStatus, "SETTLE_FAILED")
                .orderByAsc(SettlementRecordEntity::getCreatedAt));
    }

    /**
     * 查询链上状态异常的记录（用于对账）
     * <p>
     * 结算成功但链上状态不是成功，或结算处理中超过一定时间的记录。
     * </p>
     *
     * @return 异常状态结算记录列表
     */
    default List<SettlementRecordEntity> selectMismatchedRecords() {
        return selectList(new LambdaQueryWrapper<SettlementRecordEntity>()
                .and(w -> w
                        .nested(n -> n
                                .eq(SettlementRecordEntity::getStatus, "SETTLE_SUCCESS")
                                .ne(SettlementRecordEntity::getChainStatus, "CHAIN_SUCCESS"))
                        .or()
                        .eq(SettlementRecordEntity::getStatus, "SETTLE_PROCESSING"))
                .orderByAsc(SettlementRecordEntity::getCreatedAt));
    }
}
