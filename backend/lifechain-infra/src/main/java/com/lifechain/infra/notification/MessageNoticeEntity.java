package com.lifechain.infra.notification;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息通知实体
 * <p>
 * 对应数据库 message_notice 表，记录系统内所有通知消息。
 * 继承 BaseEntity 统一主键、软删除和时间字段。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_notice")
public class MessageNoticeEntity extends BaseEntity {

    /** 通知编号（唯一业务标识） */
    @TableField("notice_no")
    private String noticeNo;

    /** 接收账户ID */
    @TableField("account_id")
    private Long accountId;

    /** 通知标题 */
    @TableField("title")
    private String title;

    /** 通知内容 */
    @TableField("content")
    private String content;

    /** 通知类型（参见 NoticeType 枚举） */
    @TableField("notice_type")
    private String noticeType;

    /** 业务类型 */
    @TableField("biz_type")
    private String bizType;

    /** 业务编号 */
    @TableField("biz_no")
    private String bizNo;

    /** 已读标记：0-未读，1-已读 */
    @TableField("read_flag")
    private Integer readFlag;

    /** 阅读时间（UTC） */
    @TableField("read_time")
    private LocalDateTime readTime;

    /** 发送时间（UTC） */
    @TableField("send_time")
    private LocalDateTime sendTime;
}
