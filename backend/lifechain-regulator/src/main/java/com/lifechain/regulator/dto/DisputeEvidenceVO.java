package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 争议证据视图对象
 * <p>
 * 展示争议案件关联的证据信息，包含证据类型、文件地址、文件哈希和提交信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class DisputeEvidenceVO implements Serializable {

    /** 提交方角色 */
    private String submitterRole;

    /** 证据类型 */
    private String evidenceType;

    /** 证据描述 */
    private String evidenceDescription;

    /** 文件地址 */
    private String fileUrl;

    /** 文件哈希 */
    private String fileHash;

    /** 提交时间 */
    private LocalDateTime submitTime;
}
