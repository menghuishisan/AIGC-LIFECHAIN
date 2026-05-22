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
 * 对应数据库表 {@code work_similarity_check}。在特征提取完成后通过 Milvus ANN 检索同算法 collection，
 * 计算汉明距离并换算为相似度分数（{@code 1 - hamming_distance / 256}）。
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

    /** 算法（PDQ/MINHASH/D2） */
    @TableField("algo")
    private String algo;

    /** 汉明距离（0~256） */
    @TableField("hamming_distance")
    private Integer hammingDistance;

    /** 相似度分数（1 - distance/256） */
    @TableField("similarity_score")
    private BigDecimal similarityScore;

    /** 检测结果（PASS/HIGH_RISK/MANUAL_REVIEW） */
    @TableField("check_result")
    private String checkResult;

    /** 检测时间（UTC） */
    @TableField("check_time")
    private LocalDateTime checkTime;
}
