package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求
 * <p>
 * 包含手机号、密码、昵称、账户类型等注册必要信息。
 * 密码长度8~32位，手机号为11位数字格式。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class RegisterRequest implements Serializable {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度须在8~32位之间")
    private String password;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过64个字符")
    private String nickname;

    /** 账户类型，默认为 PERSONAL */
    private String accountType = "PERSONAL";

    /** 手机验证码（注册时必填，通过 /api/auth/sms/send 获取） */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为6位数字")
    private String smsCode;

    /** 请求幂等ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
