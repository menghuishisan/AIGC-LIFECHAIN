package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 验证查询日志实体
 * <p>
 * 对应数据库表 {@code verify_query_log}，记录所有验真查询的日志信息，
 * 包括查询类型（证书编号/作品编号/文件哈希）、查询来源（公开/登录/监管）、
 * 查询人信息、匹配结果等。用于追踪验真查询行为和审计。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("verify_query_log")
public class VerifyQueryLogEntity extends BaseEntity {

    /** 查询类型（CERT_NO/WORK_NO/FILE_HASH） */
    @TableField("query_type")
    private String queryType;

    /** 查询值 */
    @TableField("query_value")
    private String queryValue;

    /** 来源（PUBLIC/LOGIN/REGULATOR） */
    @TableField("query_source")
    private String querySource;

    /** 查询人账户ID（可为空） */
    @TableField("query_account_id")
    private Long queryAccountId;

    /** 查询IP */
    @TableField("query_ip")
    private String queryIp;

    /** 是否匹配到（0-否 1-是） */
    @TableField("match_found")
    private Integer matchFound;

    /** 结果摘要 */
    @TableField("result_summary")
    private String resultSummary;

    /** 查询时间（UTC） */
    @TableField("query_time")
    private LocalDateTime queryTime;
}
