package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 实名认证提交请求
 * <p>
 * 用户提交实名认证所需的主体信息，包括主体类型、真实姓名、
 * 证件信息、企业信息及认证材料。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class AuthSubmitRequest implements Serializable {

    /** 主体类型（PERSONAL/ENTERPRISE） */
    @NotBlank(message = "主体类型不能为空")
    private String subjectType;

    /** 真实姓名/企业名称 */
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64, message = "真实姓名不能超过64个字符")
    private String realName;

    /** 证件类型 */
    @NotBlank(message = "证件类型不能为空")
    private String idCardType;

    /** 证件号码 */
    @NotBlank(message = "证件号码不能为空")
    @Size(max = 64, message = "证件号码不能超过64个字符")
    private String idCardNo;

    /** 企业统一社会信用代码（企业账户必填） */
    private String enterpriseCode;

    /** 企业联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 认证材料文件地址 */
    private String authMaterialUrl;

    /** 请求幂等ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
