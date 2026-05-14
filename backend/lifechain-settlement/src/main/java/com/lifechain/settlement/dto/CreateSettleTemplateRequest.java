package com.lifechain.settlement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建结算模板请求
 * <p>
 * 包含模板名称、描述及分账明细列表，明细比例之和必须等于1（100%）。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class CreateSettleTemplateRequest implements Serializable {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称最长128个字符")
    private String templateName;

    /** 描述 */
    @Size(max = 512, message = "描述最长512个字符")
    private String description;

    /** 模板明细列表 */
    @NotEmpty(message = "模板明细不能为空")
    @Valid
    private List<TemplateItemDTO> items;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
