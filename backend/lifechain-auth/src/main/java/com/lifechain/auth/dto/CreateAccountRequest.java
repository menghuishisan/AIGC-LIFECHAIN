package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员创建账户请求
 * <p>
 * 由平台管理员后台直接创建 PLATFORM_ADMIN 或 REGULATOR 账户，
 * 无需短信验证码，账户类型由接口路径决定。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class CreateAccountRequest implements Serializable {

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

    /** 请求幂等ID */
    @NotBlank(message = "请求ID不能为空")
    private String requestId;
}
