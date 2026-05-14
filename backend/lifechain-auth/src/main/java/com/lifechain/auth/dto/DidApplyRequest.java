package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * DID申请请求
 * <p>
 * 已通过实名认证的用户申请数字身份（DID），
 * 系统自动生成 DID 编号和 DID 值。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class DidApplyRequest implements Serializable {

    /** 请求幂等ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
