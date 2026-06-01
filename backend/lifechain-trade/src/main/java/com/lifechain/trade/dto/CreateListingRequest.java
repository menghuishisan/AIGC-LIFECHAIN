package com.lifechain.trade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建上架请求
 * <p>
 * 用于将已确权作品发布到交易市场。
 * 可直接指定授权参数，也可引用授权模板编码快速填充。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class CreateListingRequest implements Serializable {

    /** 作品编号（由 controller 从路径变量注入，非请求体字段） */
    private String workNo;

    /** 授权模板编码（可选，传入后将自动填充授权参数） */
    private String licenseTemplateCode;

    /** 授权类型（PERSONAL_USE/COMMERCIAL_USE/EXCLUSIVE） */
    private String licenseType;

    /** 价格（单位：分） */
    @Positive(message = "价格必须大于0")
    private Long priceAmount;

    /** 范围描述 */
    private String scopeDescription;

    /** 有效天数 */
    private Integer durationDays;

    /** 幂等请求ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
