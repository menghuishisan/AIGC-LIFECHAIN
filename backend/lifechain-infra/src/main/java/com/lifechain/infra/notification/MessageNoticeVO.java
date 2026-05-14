package com.lifechain.infra.notification;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知视图对象
 */
@Data
public class MessageNoticeVO {
    private String noticeNo;
    private String title;
    private String content;
    private String noticeType;
    private String bizType;
    private String bizNo;
    private Integer readFlag;
    private LocalDateTime readTime;
    private LocalDateTime sendTime;

    public static MessageNoticeVO fromEntity(MessageNoticeEntity entity) {
        MessageNoticeVO vo = new MessageNoticeVO();
        vo.setNoticeNo(entity.getNoticeNo());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setNoticeType(entity.getNoticeType());
        vo.setBizType(entity.getBizType());
        vo.setBizNo(entity.getBizNo());
        vo.setReadFlag(entity.getReadFlag());
        vo.setReadTime(entity.getReadTime());
        vo.setSendTime(entity.getSendTime());
        return vo;
    }
}
