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
 * 对应数据库表 {@code work_feature}。所有作品类型统一输出 256-bit 二进制指纹（hex 编码 64 字符），
 * 算法分为 PDQ（图像/视频）、MINHASH（音频/文本）、D2（3D 模型）。
 * generation_fingerprint 用于识别同源 AIGC 生成（model+prompt+seed 的 sha256），避免误判。
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

    /** 作品类型（IMAGE/VIDEO/AUDIO/TEXT/MODEL） */
    @TableField("work_type")
    private String workType;

    /** 算法（PDQ/MINHASH/D2） */
    @TableField("algo")
    private String algo;

    /** 算法版本 */
    @TableField("algo_version")
    private String algoVersion;

    /** 向量维度（统一 256 bit） */
    @TableField("vector_dim")
    private Integer vectorDim;

    /** 256-bit 二进制指纹（hex 编码 64 字符） */
    @TableField("perceptual_hash")
    private String perceptualHash;

    /** AIGC 来源指纹（model+prompt+seed 的 sha256） */
    @TableField("generation_fingerprint")
    private String generationFingerprint;

    /** 提取状态（PENDING/SUCCESS/FAILED） */
    @TableField("extract_status")
    private String extractStatus;

    /** 提取时间（UTC） */
    @TableField("extract_time")
    private LocalDateTime extractTime;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 算法专属辅助信息（JSON 字符串，如 PDQ quality、视频帧数、文本字数） */
    @TableField("extra")
    private String extra;
}
