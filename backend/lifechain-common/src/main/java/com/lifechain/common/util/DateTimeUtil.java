package com.lifechain.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类
 * <p>
 * 统一使用UTC存储和处理时间，对外输出ISO 8601 UTC格式。
 * </p>
 */
public final class DateTimeUtil {

    /** UTC时区 */
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    /** ISO 8601 UTC格式化器 */
    public static final DateTimeFormatter ISO_UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(UTC_ZONE);

    private DateTimeUtil() {
    }

    /**
     * 获取当前UTC时间
     *
     * @return UTC当前时间
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC_ZONE);
    }

    /**
     * 格式化为ISO 8601 UTC字符串
     *
     * @param dateTime UTC时间
     * @return 格式化后的字符串
     */
    public static String formatUtc(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * 从ISO 8601字符串解析UTC时间
     *
     * @param isoString ISO 8601格式字符串
     * @return UTC时间
     */
    public static LocalDateTime parseUtc(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(isoString, DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * 时间戳转UTC时间
     *
     * @param epochMillis 毫秒时间戳
     * @return UTC时间
     */
    public static LocalDateTime fromEpochMillis(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), UTC_ZONE);
    }

    /**
     * UTC时间转时间戳
     *
     * @param dateTime UTC时间
     * @return 毫秒时间戳
     */
    public static long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
