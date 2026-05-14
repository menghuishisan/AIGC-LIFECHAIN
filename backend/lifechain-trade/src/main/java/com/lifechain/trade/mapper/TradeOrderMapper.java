package com.lifechain.trade.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.trade.entity.TradeOrderEntity;
import com.lifechain.common.util.DateTimeUtil;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 交易订单数据访问层
 * <p>
 * 提供交易订单表的基础CRUD操作，以及常用的按编号、买方账户、创作者账户和状态查询方法。
 * </p>
 *
 * @author LifeChain
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrderEntity> {

    /**
     * 根据订单编号查询
     *
     * @param orderNo 订单编号
     * @return 订单实体，不存在则返回null
     */
    default TradeOrderEntity selectByOrderNo(@Param("orderNo") String orderNo) {
        return selectOne(new LambdaQueryWrapper<TradeOrderEntity>()
                .eq(TradeOrderEntity::getOrderNo, orderNo));
    }

    /**
     * 根据买方账户ID查询订单列表
     *
     * @param buyerAccountId 买方账户ID
     * @return 订单列表
     */
    default java.util.List<TradeOrderEntity> selectByBuyerAccountId(@Param("buyerAccountId") Long buyerAccountId) {
        return selectList(new LambdaQueryWrapper<TradeOrderEntity>()
                .eq(TradeOrderEntity::getBuyerAccountId, buyerAccountId)
                .orderByDesc(TradeOrderEntity::getCreatedAt));
    }

    /**
     * 根据创作者账户ID查询订单列表
     *
     * @param creatorAccountId 创作者账户ID
     * @return 订单列表
     */
    default java.util.List<TradeOrderEntity> selectByCreatorAccountId(@Param("creatorAccountId") Long creatorAccountId) {
        return selectList(new LambdaQueryWrapper<TradeOrderEntity>()
                .eq(TradeOrderEntity::getCreatorAccountId, creatorAccountId)
                .orderByDesc(TradeOrderEntity::getCreatedAt));
    }

    /**
     * 查询已过期未支付的订单
     * <p>条件：订单状态为 ORDER_CREATED 且 expireTime 早于当前时间</p>
     *
     * @return 过期订单列表
     */
    default java.util.List<TradeOrderEntity> selectExpiredOrders() {
        return selectList(new LambdaQueryWrapper<TradeOrderEntity>()
                .eq(TradeOrderEntity::getOrderStatus, "ORDER_CREATED")
                .lt(TradeOrderEntity::getExpireTime, DateTimeUtil.nowUtc()));
    }

    /**
     * 按买方账户ID和订单状态分页查询
     *
     * @param page           分页参数
     * @param buyerAccountId 买方账户ID
     * @param orderStatus    订单状态（可选）
     * @return 分页结果
     */
    default IPage<TradeOrderEntity> selectByBuyerAccountIdAndStatus(Page<TradeOrderEntity> page,
                                                                     @Param("buyerAccountId") Long buyerAccountId,
                                                                     @Param("orderStatus") String orderStatus) {
        LambdaQueryWrapper<TradeOrderEntity> wrapper = new LambdaQueryWrapper<TradeOrderEntity>()
                .eq(TradeOrderEntity::getBuyerAccountId, buyerAccountId)
                .eq(orderStatus != null, TradeOrderEntity::getOrderStatus, orderStatus)
                .orderByDesc(TradeOrderEntity::getCreatedAt);
        return selectPage(page, wrapper);
    }

    /**
     * 按创作者账户ID和订单状态分页查询
     *
     * @param page              分页参数
     * @param creatorAccountId  创作者账户ID
     * @param orderStatus       订单状态（可选）
     * @return 分页结果
     */
    default IPage<TradeOrderEntity> selectByCreatorAccountIdAndStatus(Page<TradeOrderEntity> page,
                                                                      @Param("creatorAccountId") Long creatorAccountId,
                                                                      @Param("orderStatus") String orderStatus) {
        LambdaQueryWrapper<TradeOrderEntity> wrapper = new LambdaQueryWrapper<TradeOrderEntity>()
                .eq(TradeOrderEntity::getCreatorAccountId, creatorAccountId)
                .eq(orderStatus != null, TradeOrderEntity::getOrderStatus, orderStatus)
                .orderByDesc(TradeOrderEntity::getCreatedAt);
        return selectPage(page, wrapper);
    }
}
