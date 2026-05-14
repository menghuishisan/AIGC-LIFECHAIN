package com.lifechain.chain.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 链上交易提交结果
 * <p>
 * 封装 Fabric 交易提交后的回执信息，包括交易哈希、区块高度、背书摘要等。
 * 业务层根据此结果决定后续状态流转。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ChainSubmitResult {

    /** 是否成功 */
    private boolean success;

    /** 交易哈希（Fabric Transaction ID） */
    private String txHash;

    /** 区块高度 */
    private Long blockHeight;

    /** 通道名称 */
    private String channelName;

    /** 链码名称 */
    private String chaincodeName;

    /** 背书摘要（参与背书的 Peer 节点信息） */
    private String endorsementSummary;

    /** 响应负载（链码返回的 JSON 字符串） */
    private String responsePayload;

    /** 失败原因 */
    private String failReason;

    /** 原因码（对应 ErrorCodeEnum 中的链上错误分段） */
    private String reasonCode;

    /** 提交时间（UTC） */
    private LocalDateTime submitTime;

    /** 确认时间（UTC，收到回执的时间） */
    private LocalDateTime confirmTime;
}
