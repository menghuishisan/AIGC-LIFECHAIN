package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作品特征视图对象
 * <p>
 * 返回作品特征提取结果的摘要信息，包括特征类型、感知哈希、提取状态和时间。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class WorkFeatureVO implements Serializable {

    /** 特征类型（PERCEPTUAL_HASH/FINGERPRINT/VECTOR） */
    private String featureType;

    /** 感知哈希 */
    private String perceptualHash;

    /** 提取状态（PENDING/SUCCESS/FAILED） */
    private String extractStatus;

    /** 提取时间（UTC） */
    private LocalDateTime extractTime;
}
