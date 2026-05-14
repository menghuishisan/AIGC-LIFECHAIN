package com.lifechain.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 冻结申请请求
 * <p>
 * 包含冻结目标信息、冻结模式和冻结原因，用于发起对账户、作品或订单的冻结操作。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class FreezeRequest implements Serializable {

    /** 目标类型（ACCOUNT/WORK/ORDER等） */
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    /** 目标业务编号 */
    @NotBlank(message = "目标编号不能为空")
    private String targetNo;

    /** 冻结模式（REVIEW_REQUIRED/REGULATOR_DIRECT） */
    @NotBlank(message = "冻结模式不能为空")
    private String freezeMode;

    /** 冻结原因 */
    @NotBlank(message = "冻结原因不能为空")
    @Size(max = 512, message = "冻结原因最长512个字符")
    private String freezeReason;

    /** 紧急依据编号（监管直接冻结时需要） */
    private String urgentBasisNo;

    /** 原因码 */
    private String reasonCode;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
