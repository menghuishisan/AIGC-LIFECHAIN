package com.lifechain.trade.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 授权记录实体
 * <p>
 * 对应数据库表 {@code license_record}，记录每次作品授权交易生成的授权许可信息，
 * 包括授权双方、授权类型与范围、链上存证状态、生效与到期时间等。
 * 授权记录在链上完成存证后具备法律效力，支持跨平台验证。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("license_record")
public class LicenseRecordEntity extends BaseEntity {

    /** 授权编号（对外唯一标识） */
    @TableField("license_no")
    private String licenseNo;

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 作品ID */
    @TableField("work_id")
    private Long workId;

    /** 作品编号 */
    @TableField("work_no")
    private String workNo;

    /** 授权方账户ID（创作者） */
    @TableField("licensor_account_id")
    private Long licensorAccountId;

    /** 被授权方账户ID（买方） */
    @TableField("licensee_account_id")
    private Long licenseeAccountId;

    /** 授权类型 */
    @TableField("license_type")
    private String licenseType;

    /** 授权状态 */
    @TableField("license_status")
    private String licenseStatus;

    /** 链上状态 */
    @TableField("chain_status")
    private String chainStatus;

    /** 范围描述 */
    @TableField("scope_description")
    private String scopeDescription;

    /** 生效时间（UTC） */
    @TableField("effective_time")
    private LocalDateTime effectiveTime;

    /** 到期时间（UTC） */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 授权哈希 */
    @TableField("license_hash")
    private String licenseHash;

    /** 交易哈希（Fabric） */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;

    /** 终止时间（UTC） */
    @TableField("terminate_time")
    private LocalDateTime terminateTime;

    /** 终止原因 */
    @TableField("terminate_reason")
    private String terminateReason;

    /** 幂等请求ID */
    @TableField("request_id")
    private String requestId;
}
