package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 * <p>
 * 包含手机号和密码，用于用户身份验证。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class LoginRequest implements Serializable {

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
