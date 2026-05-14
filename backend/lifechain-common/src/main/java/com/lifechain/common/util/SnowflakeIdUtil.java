package com.lifechain.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 雪花ID生成工具
 * <p>
 * 所有主键统一使用雪花ID生成，确保分布式环境下的全局唯一性。
 * </p>
 */
public final class SnowflakeIdUtil {

    /** 雪花ID生成器实例（工作机器ID=1，数据中心ID=1） */
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private SnowflakeIdUtil() {
    }

    /**
     * 生成雪花ID
     *
     * @return 雪花ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 生成雪花ID字符串
     *
     * @return 雪花ID字符串
     */
    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }
}
