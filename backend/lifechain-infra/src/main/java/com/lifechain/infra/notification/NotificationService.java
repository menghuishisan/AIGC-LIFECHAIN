package com.lifechain.infra.notification;

import com.lifechain.common.model.PageResult;

import java.util.List;

/**
 * 消息通知服务接口
 * <p>
 * 定义系统内消息通知的发送与查询契约，支持单条和批量发送、分页查询、已读标记。
 * 通知消息落库持久化，可作为站内信或推送基础数据。
 * </p>
 */
public interface NotificationService {

    /**
     * 发送单条通知
     */
    void sendNotice(Long accountId, String title, String content,
                    String noticeType, String bizType, String bizNo);

    /**
     * 批量发送通知
     */
    void sendBatchNotice(List<Long> accountIds, String title, String content,
                         String noticeType, String bizType, String bizNo);

    /**
     * 分页查询指定账户的通知列表
     */
    PageResult<MessageNoticeVO> listNotices(Long accountId, String noticeType,
                                            Integer readFlag, int pageNo, int pageSize);

    /**
     * 标记通知已读
     */
    void markRead(Long accountId, String noticeNo);

    /**
     * 查询通知详情
     *
     * @param accountId 账户ID
     * @param noticeNo  通知编号
     * @return 通知详情
     */
    MessageNoticeVO getNoticeDetail(Long accountId, String noticeNo);
}
