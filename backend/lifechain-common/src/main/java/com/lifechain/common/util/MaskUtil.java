package com.lifechain.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * 数据脱敏工具
 * <p>
 * 用于敏感信息脱敏展示，如手机号、身份证号、姓名等。
 * </p>
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /**
     * 手机号脱敏（保留前3后4）
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号，如 138****1234
     */
    public static String maskMobile(String mobile) {
        if (StrUtil.isBlank(mobile) || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    /**
     * 身份证号脱敏（保留前4后4）
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard) || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 姓名脱敏（保留姓）
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (StrUtil.isBlank(name)) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 邮箱脱敏（保留首字母和@后域名）
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email) || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
