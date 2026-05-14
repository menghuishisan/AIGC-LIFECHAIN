package com.lifechain.common.util;

import cn.hutool.crypto.digest.DigestUtil;

import java.io.InputStream;

/**
 * 哈希计算工具
 * <p>
 * 提供SHA-256等哈希计算方法，用于文件哈希、元数据哈希、摘要哈希等场景。
 * </p>
 */
public final class HashUtil {

    private HashUtil() {
    }

    /**
     * 计算字符串的SHA-256哈希
     *
     * @param content 字符串内容
     * @return SHA-256哈希值（十六进制）
     */
    public static String sha256(String content) {
        return DigestUtil.sha256Hex(content);
    }

    /**
     * 计算字节数组的SHA-256哈希
     *
     * @param data 字节数组
     * @return SHA-256哈希值（十六进制）
     */
    public static String sha256(byte[] data) {
        return DigestUtil.sha256Hex(data);
    }

    /**
     * 计算输入流的SHA-256哈希
     *
     * @param inputStream 输入流
     * @return SHA-256哈希值（十六进制）
     */
    public static String sha256(InputStream inputStream) {
        return DigestUtil.sha256Hex(inputStream);
    }

    /**
     * 计算字符串的MD5哈希
     *
     * @param content 字符串内容
     * @return MD5哈希值（十六进制）
     */
    public static String md5(String content) {
        return DigestUtil.md5Hex(content);
    }
}
