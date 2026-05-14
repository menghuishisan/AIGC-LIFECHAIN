package com.lifechain.common.context;

/**
 * 当前登录用户上下文工具
 * <p>
 * 基于ThreadLocal存储当前请求的登录用户信息，供业务层获取当前操作人。
 * 在请求完成后必须清除以防内存泄漏。
 * </p>
 */
public final class UserContext {

    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前用户
     *
     * @param userInfo 用户信息
     */
    public static void set(UserInfo userInfo) {
        CURRENT_USER.set(userInfo);
    }

    /**
     * 获取当前用户
     *
     * @return 当前用户信息，未登录时返回null
     */
    public static UserInfo get() {
        return CURRENT_USER.get();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        UserInfo info = get();
        return info != null ? info.getUserId() : null;
    }

    /**
     * 获取当前用户账户编号
     *
     * @return 账户编号
     */
    public static String getAccountNo() {
        UserInfo info = get();
        return info != null ? info.getAccountNo() : null;
    }

    /**
     * 清除当前用户（请求完成后必须调用）
     */
    public static void clear() {
        CURRENT_USER.remove();
    }

    /**
     * 用户信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserInfo {
        /** 用户ID */
        private Long userId;
        /** 账户编号 */
        private String accountNo;
        /** 账户类型 */
        private String accountType;
        /** 角色列表 */
        private java.util.List<String> roles;
    }
}
