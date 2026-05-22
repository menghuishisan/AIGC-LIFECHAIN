package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作品特征视图对象
 * <p>
 * 返回作品特征提取结果的摘要信息，包括算法、256-bit 二进制指纹（hex 编码）、
 * AIGC 来源指纹、提取状态和时间。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class WorkFeatureVO implements Serializable {

    /** 作品类型（IMAGE/VIDEO/AUDIO/TEXT/MODEL） */
    private String workType;

    /** 算法（PDQ/MINHASH/D2） */
    private String algo;

    /** 算法版本 */
    private String algoVersion;

    /** 256-bit 二进制指纹（hex 编码 64 字符） */
    private String perceptualHash;

    /** AIGC 来源指纹（同 model+prompt+seed 不视为抄袭） */
    private String generationFingerprint;

    /** 提取状态（PENDING/SUCCESS/FAILED） */
    private String extractStatus;

    /** 提取时间（UTC） */
    private LocalDateTime extractTime;
}
