package com.lifechain.chain.record;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 区块链交易记录实体
 * <p>
 * 对应数据库表 {@code chain_tx_record}，记录每一次链上交易的完整生命周期信息。
 * 从提交请求到收到回执，所有状态变更均可追溯。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chain_tx_record")
public class ChainTxRecordEntity extends BaseEntity {

    /** 业务类型（对应 BizTypeEnum） */
    @TableField("biz_type")
    private String bizType;

    /** 业务ID */
    @TableField("biz_id")
    private Long bizId;

    /** 业务编号 */
    @TableField("biz_no")
    private String bizNo;

    /** 交易类型：REGISTER / UPDATE / QUERY */
    @TableField("tx_type")
    private String txType;

    /** 通道名称 */
    @TableField("channel_name")
    private String channelName;

    /** 链码名称 */
    @TableField("chaincode_name")
    private String chaincodeName;

    /** 交易哈希（Fabric Transaction ID） */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;

    /** 链上状态（对应 ChainStatusEnum） */
    @TableField("chain_status")
    private String chainStatus;

    /** 请求负载哈希（SHA-256） */
    @TableField("request_payload_hash")
    private String requestPayloadHash;

    /** 响应负载（链码返回的 JSON） */
    @TableField("response_payload")
    private String responsePayload;

    /** 背书摘要 */
    @TableField("endorsement_summary")
    private String endorsementSummary;

    /** 失败原因 */
    @TableField("fail_reason")
    private String failReason;

    /** 原因码 */
    @TableField("reason_code")
    private String reasonCode;

    /** 提交时间（UTC） */
    @TableField("submit_time")
    private LocalDateTime submitTime;

    /** 确认时间（UTC） */
    @TableField("confirm_time")
    private LocalDateTime confirmTime;
}
