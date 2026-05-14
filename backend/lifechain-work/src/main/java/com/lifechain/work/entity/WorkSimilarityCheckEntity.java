package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品相似度检测实体
 * <p>
 * 对应数据库表 {@code work_similarity_check}，存储作品之间的相似度检测结果，
 * 包括待检作品与对比作品的相似度分数、检测结果（通过/高风险/需人工审核）等。
 * 在特征提取完成后自动触发与已有作品的相似度比对。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_similarity_check")
public class WorkSimilarityCheckEntity extends BaseEntity {

    /** 待检作品ID */
    @TableField("work_id")
    private Long workId;

    /** 对比作品ID */
    @TableField("compared_work_id")
    private Long comparedWorkId;

    /** 相似度分数 */
    @TableField("similarity_score")
    private BigDecimal similarityScore;

    /** 检测结果（PASS/HIGH_RISK/MANUAL_REVIEW） */
    @TableField("check_result")
    private String checkResult;

    /** 检测时间（UTC） */
    @TableField("check_time")
    private LocalDateTime checkTime;
}
