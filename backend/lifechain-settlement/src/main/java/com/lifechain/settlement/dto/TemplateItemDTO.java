package com.lifechain.settlement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 模板明细DTO
 * <p>
 * 定义单个角色在结算模板中的分账比例配置。
 * 比例范围为 0.0001 ~ 1.0000，精确到万分之一。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class TemplateItemDTO implements Serializable {

    /** 角色类型（CREATOR/PLATFORM/OTHER） */
    @NotBlank(message = "角色类型不能为空")
    private String roleType;

    /** 分账比例（0.0001~1.0000，如0.8000表示80%） */
    @NotNull(message = "分账比例不能为空")
    @DecimalMin(value = "0.0001", message = "分账比例最小为0.0001")
    @DecimalMax(value = "1.0000", message = "分账比例最大为1.0000")
    private BigDecimal ratio;

    /** 描述 */
    private String description;
}
