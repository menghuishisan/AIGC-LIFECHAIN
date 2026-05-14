package com.lifechain.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算模板视图对象
 * <p>
 * 包含模板基本信息及其明细列表，用于模板详情和列表查询的返回。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class SettleTemplateVO implements Serializable {

    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 描述 */
    private String description;

    /** 状态 */
    private String status;

    /** 模板明细列表 */
    private List<TemplateItemDTO> items;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
