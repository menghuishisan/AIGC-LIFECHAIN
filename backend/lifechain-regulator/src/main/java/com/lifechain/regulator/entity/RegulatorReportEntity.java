package com.lifechain.regulator.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lifechain.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 监管报告实体
 * <p>
 * 对应数据库表 {@code regulator_report}，记录监管报告的生成与链上存证信息，
 * 包含报告类型、内容、文件地址、生成状态以及链上状态等核心字段。
 * 报告生命周期：草稿 → 生成中 → 完成/失败 → 链上存证。
 * </p>
 *
 * @author LifeChain
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("regulator_report")
public class RegulatorReportEntity extends BaseEntity {

    /** 报告编号（对外唯一标识） */
    @TableField("report_no")
    private String reportNo;

    /** 报告类型 */
    @TableField("report_type")
    private String reportType;

    /** 报告标题 */
    @TableField("report_title")
    private String reportTitle;

    /** 报告内容 */
    @TableField("report_content")
    private String reportContent;

    /** 报告文件地址 */
    @TableField("report_file_url")
    private String reportFileUrl;

    /** 生成人ID */
    @TableField("generator_id")
    private Long generatorId;

    /** 状态（DRAFT/GENERATING/COMPLETED/FAILED） */
    @TableField("status")
    private String status;

    /** 生成时间（UTC） */
    @TableField("generate_time")
    private LocalDateTime generateTime;

    /** 摘要哈希 */
    @TableField("summary_hash")
    private String summaryHash;

    /** 链上状态 */
    @TableField("chain_status")
    private String chainStatus;

    /** 交易哈希 */
    @TableField("tx_hash")
    private String txHash;

    /** 区块高度 */
    @TableField("block_height")
    private Long blockHeight;
}
