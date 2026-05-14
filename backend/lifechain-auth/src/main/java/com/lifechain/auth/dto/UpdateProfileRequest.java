package com.lifechain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 更新个人资料请求
 * <p>
 * 用于修改当前登录用户的昵称、邮箱、头像等非敏感信息。
 * </p>
 *
 * @author LifeChain
 */
@Data
public class UpdateProfileRequest implements Serializable {

    /** 昵称 */
    @Size(max = 64, message = "昵称不能超过64个字符")
    private String nickname;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 头像地址 */
    @Size(max = 512, message = "头像地址不能超过512个字符")
    private String avatarUrl;

    /** 幂等请求标识 */
    @jakarta.validation.constraints.NotBlank(message = "requestId不能为空")
    private String requestId;
}
