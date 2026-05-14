package com.lifechain.common.enums;

/**
 * 接口调用方角色（用于字段可见性矩阵）
 * <p>
 * 对应文档 §7 字段可见性矩阵的四列：公开接口、登录接口、平台管理员接口、监管接口。
 * </p>
 */
public enum ViewerRole {
    /** 公开接口（未登录） */
    PUBLIC,
    /** 普通登录用户 */
    USER,
    /** 平台管理员 */
    ADMIN,
    /** 监管员 */
    REGULATOR
}
