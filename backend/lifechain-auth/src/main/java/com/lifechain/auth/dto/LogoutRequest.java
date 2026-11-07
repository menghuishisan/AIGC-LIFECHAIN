package com.lifechain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 退出登录同时撤销当前刷新凭证，防止仅清理访问令牌后仍能续期。 */
public record LogoutRequest(@NotBlank String refreshToken) {
}
