package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 账户冻结/解冻请求
 * <p>
 * 管理员对账户执行冻结或解冻操作，需提供目标账户编号和原因。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class AccountFreezeRequest implements Serializable {

    /** 账户编号 */
    @NotBlank(message = "账户编号不能为空")
    private String accountNo;

    /** 原因 */
    @NotBlank(message = "操作原因不能为空")
    private String reason;

    /** 原因码 */
    private String reasonCode;

    /** 请求幂等ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
