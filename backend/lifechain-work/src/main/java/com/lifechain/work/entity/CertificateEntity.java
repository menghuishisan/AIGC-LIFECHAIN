package com.lifechain.work.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 证书实体
 * <p>
 * 对应数据库表 {@code certificate}，存储确权成功后生成的数字版权证书信息，
 * 包括证书编号、持有人、版本号、签发/到期/吊销时间、证书文件哈希等。
 * 支持版本递增（reissue时关联上一版证书）。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("certificate")
public class CertificateEntity extends BaseEntity {

    /** 证书编号（对外唯一标识） */
    @TableField("cert_no")
    private String certNo;

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 确权申请ID */
    @TableField("claim_id")
    private Long claimId;

    /** 确权编号 */
    @TableField("claim_no")
    private String claimNo;

    /** 持有人账户ID */
    @TableField("holder_account_id")
    private Long holderAccountId;

    /** 持有人DID ID */
    @TableField("holder_did_id")
    private Long holderDidId;

    /** 证书状态 */
    @TableField("status")
    private String status;

    /** 证书哈希（SHA-256） */
    @TableField("cert_hash")
    private String certHash;

    /** 证书文件地址 */
    @TableField("cert_file_url")
    private String certFileUrl;

    /** 版本号 */
    @TableField("version")
    private Integer version;

    /** 上一版本证书ID */
    @TableField("previous_cert_id")
    private Long previousCertId;

    /** 签发时间（UTC） */
    @TableField("issue_time")
    private LocalDateTime issueTime;

    /** 到期时间（UTC） */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 吊销时间（UTC） */
    @TableField("revoke_time")
    private LocalDateTime revokeTime;

    /** 吊销原因 */
    @TableField("revoke_reason")
    private String revokeReason;
}
