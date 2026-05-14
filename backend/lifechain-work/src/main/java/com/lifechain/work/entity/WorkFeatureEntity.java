package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 作品特征实体
 * <p>
 * 对应数据库表 {@code work_feature}，存储作品的特征提取结果，
 * 包括感知哈希、指纹、向量等特征类型，以及提取状态和失败原因。
 * 特征数据用于作品查重和相似度检测。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_feature")
public class WorkFeatureEntity extends BaseEntity {

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 特征类型（PERCEPTUAL_HASH/FINGERPRINT/VECTOR） */
    @TableField("feature_type")
    private String featureType;

    /** 特征值 */
    @TableField("feature_value")
    private String featureValue;

    /** 感知哈希 */
    @TableField("perceptual_hash")
    private String perceptualHash;

    /** 提取状态（PENDING/SUCCESS/FAILED） */
    @TableField("extract_status")
    private String extractStatus;

    /** 提取时间（UTC） */
    @TableField("extract_time")
    private LocalDateTime extractTime;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;
}
