package com.lifechain.regulator.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 监管报告视图对象
 * <p>
 * 展示监管报告完整信息，包含报告类型、内容、生成状态及链上存证信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ReportVO implements Serializable {

    /** 报告编号 */
    private String reportNo;

    /** 报告类型 */
    private String reportType;

    /** 报告标题 */
    private String reportTitle;

    /** 报告内容 */
    private String reportContent;

    /** 报告文件地址 */
    private String reportFileUrl;

    /** 状态 */
    private String status;

    /** 生成时间 */
    private LocalDateTime generateTime;

    /** 摘要哈希 */
    private String summaryHash;

    /** 链上状态 */
    private String chainStatus;

    /** 交易哈希 */
    private String txHash;

    /** 区块高度 */
    private Long blockHeight;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
