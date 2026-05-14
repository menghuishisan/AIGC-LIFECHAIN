package com.lifechain.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis缓存服务
 * <p>
 * 封装Redis常用操作，包括字符串读写、Hash读写、分布式锁、计数器等。
 * 序列化统一使用Jackson ObjectMapper，保证与系统其他模块一致。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 设置缓存值（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值（自动序列化为JSON）
     * @param timeout 过期时长
     * @param unit    时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, timeout, unit);
            log.debug("Redis SET成功, key: {}, 过期: {}{}", key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis SET失败, key: {}, 错误: {}", key, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存写入失败: " + e.getMessage());
        }
    }

    /**
     * 获取原始字符串缓存值（不做JSON反序列化）
     *
     * @param key 缓存键
     * @return 原始字符串，不存在返回null
     */
    public String getString(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis GET_STRING失败, key: {}, 错误: {}", key, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存读取失败: " + e.getMessage());
        }
    }

    /**
     * 获取缓存值
     *
     * @param key   缓存键
     * @param clazz 目标类型
     * @param <T>   返回类型
     * @return 缓存值，不存在返回null
     */
    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Redis GET失败, key: {}, 错误: {}", key, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存读取失败: " + e.getMessage());
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            log.debug("Redis DELETE, key: {}, 结果: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis DELETE失败, key: {}, 错误: {}", key, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存删除失败: " + e.getMessage());
        }
    }

    /**
     * 判断缓存键是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis HASKEY失败, key: {}, 错误: {}", key, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存查询失败: " + e.getMessage());
        }
    }

    /**
     * 自增计数器
     *
     * @param key 缓存键
     * @return 自增后的值
     */
    public Long increment(String key) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            log.debug("Redis INCREMENT, key: {}, 当前值: {}", key, value);
            return value;
        } catch (Exception e) {
            log.error("Redis INCREMENT失败, key: {}, 错误: {}", key, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存计数器自增失败: " + e.getMessage());
        }
    }

    /**
     * 设置键过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时长
     * @param unit    时间单位
     */
    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis EXPIRE失败, key: {}, 错误: {}", key, e.getMessage(), e);
        }
    }

    /**
     * 仅当键不存在时设置值（分布式锁核心操作）
     * <p>
     * 利用Redis SETNX原子特性实现分布式锁和幂等校验。
     * </p>
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时长
     * @param unit    时间单位
     * @return true-设置成功（键不存在），false-键已存在
     */
    public Boolean setIfAbsent(String key, Object value, long timeout, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(value);
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, json, timeout, unit);
            log.debug("Redis SETNX, key: {}, 结果: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis SETNX失败, key: {}, 错误: {}", key, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存SETNX失败: " + e.getMessage());
        }
    }

    /**
     * 设置Hash字段值
     *
     * @param key   Hash键
     * @param field Hash字段名
     * @param value 字段值（自动序列化为JSON）
     */
    public void setHash(String key, String field, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForHash().put(key, field, json);
            log.debug("Redis HSET成功, key: {}, field: {}", key, field);
        } catch (Exception e) {
            log.error("Redis HSET失败, key: {}, field: {}, 错误: {}", key, field, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存Hash写入失败: " + e.getMessage());
        }
    }

    /**
     * 获取Hash字段值
     *
     * @param key   Hash键
     * @param field Hash字段名
     * @param clazz 目标类型
     * @param <T>   返回类型
     * @return 字段值，不存在返回null
     */
    public <T> T getHash(String key, String field, Class<T> clazz) {
        try {
            Object raw = redisTemplate.opsForHash().get(key, field);
            if (raw == null) {
                return null;
            }
            return objectMapper.readValue(raw.toString(), clazz);
        } catch (Exception e) {
            log.error("Redis HGET失败, key: {}, field: {}, 错误: {}", key, field, e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.CACHE_ERROR, "缓存Hash读取失败: " + e.getMessage());
        }
    }
}
