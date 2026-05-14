package com.lifechain.work.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 确权申请提交请求
 * <p>
 * 提交作品确权申请时的请求参数，仅需指定作品编号。
 * 系统自动关联当前登录用户的账户和DID信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class ClaimSubmitRequest implements Serializable {

    /** 作品编号 */
    @NotBlank(message = "作品编号不能为空")
    private String workNo;

    /** 幂等请求ID */
    @NotBlank(message = "requestId不能为空")
    private String requestId;
}
