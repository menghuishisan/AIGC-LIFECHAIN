package com.lifechain.settlement.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifechain.settlement.entity.SettlementItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 结算明细数据访问层
 * <p>
 * 提供结算明细表的基础CRUD操作，以及按结算记录ID和结算编号查询明细列表的方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface SettlementItemMapper extends BaseMapper<SettlementItemEntity> {

    /** 主结算确认且未进入逆转流程的金额才计入当前净收益。 */
    @Select("""
            SELECT i.*, s.status AS settle_status FROM settlement_item i
            JOIN settlement_record s ON s.id = i.settle_id
            WHERE i.account_id = #{accountId} AND i.deleted_flag = 0 AND s.deleted_flag = 0
              AND s.status = 'SETTLE_SUCCESS' AND s.chain_status = 'CHAIN_SUCCESS'
            """)
    List<SettlementItemEntity> selectConfirmedIncome(@Param("accountId") Long accountId);

    /** 收益明细的状态统一取主结算，逆分账完成后不会残留成功收益。 */
    @Select("""
            SELECT i.*, s.status AS settle_status FROM settlement_item i
            JOIN settlement_record s ON s.id = i.settle_id
            WHERE i.account_id = #{accountId} AND i.deleted_flag = 0 AND s.deleted_flag = 0
            ORDER BY i.created_at DESC, i.id DESC
            """)
    Page<SettlementItemEntity> selectIncomePage(Page<SettlementItemEntity> page, @Param("accountId") Long accountId);

    /**
     * 根据结算记录ID查询所有明细
     *
     * @param settleId 结算记录ID
     * @return 结算明细列表
     */
    default List<SettlementItemEntity> selectBySettleId(@Param("settleId") Long settleId) {
        return selectList(new LambdaQueryWrapper<SettlementItemEntity>()
                .eq(SettlementItemEntity::getSettleId, settleId)
                .orderByAsc(SettlementItemEntity::getRoleType));
    }

    /**
     * 根据结算编号查询所有明细
     *
     * @param settleNo 结算编号
     * @return 结算明细列表
     */
    default List<SettlementItemEntity> selectBySettleNo(@Param("settleNo") String settleNo) {
        return selectList(new LambdaQueryWrapper<SettlementItemEntity>()
                .eq(SettlementItemEntity::getSettleNo, settleNo)
                .orderByAsc(SettlementItemEntity::getRoleType));
    }
}
