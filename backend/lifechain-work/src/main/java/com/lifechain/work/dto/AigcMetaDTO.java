package com.lifechain.work.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AIGC元数据传输对象
 * <p>
 * 用于作品上传和元数据更新时传递AIGC生成信息，
 * 包括工具名称、模型、版本、提示词摘要、生成参数和生成时间。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class AigcMetaDTO implements Serializable {

    /** AIGC工具名称 */
    private String aigcTool;

    /** AIGC模型名称 */
    private String aigcModel;

    /** AIGC版本 */
    private String aigcVersion;

    /** 提示词摘要 */
    private String promptSummary;

    /** 生成参数（JSON字符串） */
    private String generationParams;

    /** 生成时间（ISO-8601格式字符串） */
    private String generationTime;
}
