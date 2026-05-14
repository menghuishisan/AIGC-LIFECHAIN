package com.lifechain.regulator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 争议证据实体
 * <p>
 * 对应数据库表 {@code dispute_evidence}，记录争议案件中各方提交的证据材料，
 * 包含证据类型、文件地址、文件哈希和提交时间等核心字段。
 * 文件哈希用于保障证据完整性，防止篡改。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispute_evidence")
public class DisputeEvidenceEntity extends BaseEntity {

    /** 争议案件ID */
    @TableField("case_id")
    private Long caseId;

    /** 争议案件编号 */
    @TableField("case_no")
    private String caseNo;

    /** 提交人账户ID */
    @TableField("submitter_account_id")
    private Long submitterAccountId;

    /** 证据类型 */
    @TableField("evidence_type")
    private String evidenceType;

    /** 证据描述 */
    @TableField("evidence_description")
    private String evidenceDescription;

    /** 文件地址 */
    @TableField("file_url")
    private String fileUrl;

    /** 文件哈希 */
    @TableField("file_hash")
    private String fileHash;

    /** 提交时间（UTC） */
    @TableField("submit_time")
    private LocalDateTime submitTime;
}
