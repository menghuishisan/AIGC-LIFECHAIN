package com.lifechain.infra.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.common.model.PageResult;
import com.lifechain.common.util.BizNoUtil;
import com.lifechain.common.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息通知服务实现
 * <p>
 * 将通知消息持久化至 message_notice 表，每条通知生成唯一的通知编号。
 * 支持单条和批量发送，批量发送时为每个接收账户独立生成通知记录。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 发送单条通知
     * <p>
     * 生成通知编号，构建通知实体并持久化至数据库。
     * </p>
     *
     * @param accountId  接收账户ID
     * @param title      通知标题
     * @param content    通知内容
     * @param noticeType 通知类型
     * @param bizType    业务类型
     * @param bizNo      业务编号
     */
    @Override
    public void sendNotice(Long accountId, String title, String content,
                           String noticeType, String bizType, String bizNo) {
        try {
            MessageNoticeEntity entity = buildNoticeEntity(accountId, title, content,
                    noticeType, bizType, bizNo);
            notificationMapper.insert(entity);
            log.info("通知发送成功, 通知编号: {}, 账户: {}, 标题: {}", entity.getNoticeNo(), accountId, title);
        } catch (Exception e) {
            log.error("通知发送失败, 账户: {}, 标题: {}, 错误: {}", accountId, title, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.NOTIFICATION_FAILED,
                    "通知发送失败: " + e.getMessage());
        }
    }

    /**
     * 批量发送通知
     * <p>
     * 为每个接收账户独立生成通知记录并逐条插入。
     * 部分失败不影响其他账户的通知发送。
     * </p>
     *
     * @param accountIds 接收账户ID列表
     * @param title      通知标题
     * @param content    通知内容
     * @param noticeType 通知类型
     * @param bizType    业务类型
     * @param bizNo      业务编号
     */
    @Override
    public void sendBatchNotice(List<Long> accountIds, String title, String content,
                                String noticeType, String bizType, String bizNo) {
        if (accountIds == null || accountIds.isEmpty()) {
            log.warn("批量通知: 接收账户列表为空, 跳过发送");
            return;
        }

        List<Long> failedIds = new ArrayList<>();
        for (Long accountId : accountIds) {
            try {
                MessageNoticeEntity entity = buildNoticeEntity(accountId, title, content,
                        noticeType, bizType, bizNo);
                notificationMapper.insert(entity);
            } catch (Exception e) {
                log.error("批量通知: 账户 {} 发送失败, 错误: {}", accountId, e.getMessage(), e);
                failedIds.add(accountId);
            }
        }

        if (failedIds.isEmpty()) {
            log.info("批量通知发送成功, 接收账户数: {}, 标题: {}", accountIds.size(), title);
        } else {
            log.warn("批量通知部分失败, 总数: {}, 失败数: {}, 失败账户: {}",
                    accountIds.size(), failedIds.size(), failedIds);
        }
    }

    @Override
    public PageResult<MessageNoticeVO> listNotices(Long accountId, String noticeType,
                                                   Integer readFlag, int pageNo, int pageSize) {
        LambdaQueryWrapper<MessageNoticeEntity> wrapper = new LambdaQueryWrapper<MessageNoticeEntity>()
                .eq(MessageNoticeEntity::getAccountId, accountId)
                .eq(noticeType != null, MessageNoticeEntity::getNoticeType, noticeType)
                .eq(readFlag != null, MessageNoticeEntity::getReadFlag, readFlag)
                .orderByDesc(MessageNoticeEntity::getSendTime);
        Page<MessageNoticeEntity> page = new Page<>(pageNo, pageSize);
        Page<MessageNoticeEntity> result = notificationMapper.selectPage(page, wrapper);
        List<MessageNoticeVO> voList = result.getRecords().stream()
                .map(MessageNoticeVO::fromEntity).toList();
        return PageResult.of(voList, result.getTotal(), pageNo, pageSize);
    }

    @Override
    public void markRead(Long accountId, String noticeNo) {
        MessageNoticeEntity entity = notificationMapper.selectOne(
                new LambdaQueryWrapper<MessageNoticeEntity>()
                        .eq(MessageNoticeEntity::getNoticeNo, noticeNo)
                        .eq(MessageNoticeEntity::getAccountId, accountId));
        if (entity != null && entity.getReadFlag() == 0) {
            entity.setReadFlag(1);
            entity.setReadTime(DateTimeUtil.nowUtc());
            notificationMapper.updateById(entity);
        }
    }

    @Override
    public MessageNoticeVO getNoticeDetail(Long accountId, String noticeNo) {
        MessageNoticeEntity entity = notificationMapper.selectOne(
                new LambdaQueryWrapper<MessageNoticeEntity>()
                        .eq(MessageNoticeEntity::getNoticeNo, noticeNo)
                        .eq(MessageNoticeEntity::getAccountId, accountId));
        if (entity == null) {
            throw new BizException(ErrorCodeEnum.RESOURCE_NOT_FOUND, "通知不存在: " + noticeNo);
        }
        return MessageNoticeVO.fromEntity(entity);
    }

    /**
     * 构建通知实体
     *
     * @param accountId  接收账户ID
     * @param title      通知标题
     * @param content    通知内容
     * @param noticeType 通知类型
     * @param bizType    业务类型
     * @param bizNo      业务编号
     * @return 通知实体（未持久化）
     */
    private MessageNoticeEntity buildNoticeEntity(Long accountId, String title, String content,
                                                  String noticeType, String bizType, String bizNo) {
        MessageNoticeEntity entity = new MessageNoticeEntity();
        entity.setNoticeNo(BizNoUtil.noticeNo());
        entity.setAccountId(accountId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setNoticeType(noticeType);
        entity.setBizType(bizType);
        entity.setBizNo(bizNo);
        entity.setReadFlag(0);
        entity.setSendTime(LocalDateTime.now(ZoneOffset.UTC));
        return entity;
    }
}
