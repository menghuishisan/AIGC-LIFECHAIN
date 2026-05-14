package com.lifechain.infra.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消息通知Mapper
 * <p>
 * 基于MyBatis-Plus BaseMapper提供message_notice表的基础CRUD操作，
 * 并扩展常用查询方法。
 * </p>
 */
@Mapper
public interface NotificationMapper extends BaseMapper<MessageNoticeEntity> {

    /**
     * 查询指定账户的未读通知列表
     *
     * @param accountId 账户ID
     * @return 未读通知列表（按发送时间倒序）
     */
    @Select("SELECT * FROM message_notice WHERE account_id = #{accountId} AND read_flag = 0 AND deleted_flag = 0 ORDER BY send_time DESC")
    List<MessageNoticeEntity> selectUnreadByAccountId(@Param("accountId") Long accountId);

    /**
     * 查询指定账户的未读通知数量
     *
     * @param accountId 账户ID
     * @return 未读通知数量
     */
    @Select("SELECT COUNT(*) FROM message_notice WHERE account_id = #{accountId} AND read_flag = 0 AND deleted_flag = 0")
    int countUnreadByAccountId(@Param("accountId") Long accountId);
}
