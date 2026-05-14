package com.lifechain.common.util;

import com.lifechain.common.context.UserContext;
import com.lifechain.common.enums.ViewerRole;

import java.util.List;

/**
 * 字段可见性工具类
 * <p>
 * 根据当前请求方角色，判定字段可见性级别。
 * 与文档 §7 字段可见性矩阵对齐。
 * </p>
 */
public final class FieldVisibilityUtil {

    private FieldVisibilityUtil() {
    }

    /**
     * 根据当前 UserContext 推断调用方角色
     *
     * @return 当前请求的 ViewerRole
     */
    public static ViewerRole resolveViewerRole() {
        UserContext.UserInfo userInfo = UserContext.get();
        if (userInfo == null || userInfo.getRoles() == null || userInfo.getRoles().isEmpty()) {
            return ViewerRole.PUBLIC;
        }
        List<String> roles = userInfo.getRoles();
        if (roles.contains("PLATFORM_ADMIN")) {
            return ViewerRole.ADMIN;
        }
        if (roles.contains("REGULATOR")) {
            return ViewerRole.REGULATOR;
        }
        return ViewerRole.USER;
    }

    /**
     * 判断当前调用方是否为特权角色（管理员或监管员）
     * <p>
     * 特权角色可查看完整的敏感字段（手机号、身份证号、真实姓名等），
     * 非特权角色需要脱敏处理。统一所有服务的脱敏判断入口。
     * </p>
     *
     * @return true 表示管理员或监管员，不需要脱敏；false 表示需要脱敏
     */
    public static boolean isPrivilegedViewer() {
        ViewerRole role = resolveViewerRole();
        return role == ViewerRole.ADMIN || role == ViewerRole.REGULATOR;
    }
}
