package com.lifechain.infra.idempotent;

import com.lifechain.common.annotation.Idempotent;
import com.lifechain.common.context.UserContext;
import com.lifechain.common.enums.ErrorCodeEnum;
import com.lifechain.common.exception.BizException;
import com.lifechain.infra.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/**
 * 幂等请求切面
 * <p>
 * 拦截所有标注了 {@link Idempotent} 注解的方法，通过Redis SETNX实现幂等校验。
 * 从方法第一个参数中反射提取 requestId 字段作为幂等键，
 * 在指定窗口时间内相同requestId的重复请求将被拒绝。
 * </p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    /** 幂等键前缀 */
    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:";

    private final RedisService redisService;

    /**
     * 环绕通知：幂等校验
     * <p>
     * 1. 从第一个方法参数中通过反射获取requestId字段值<br>
     * 2. 使用Redis SETNX原子操作判断该requestId是否已处理<br>
     * 3. 若已存在则抛出重复请求异常<br>
     * 4. 若不存在则放行，执行目标方法
     * </p>
     *
     * @param joinPoint  切入点
     * @param idempotent 幂等注解
     * @return 目标方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String requestId = extractRequestId(joinPoint);
        if (requestId == null || requestId.isBlank()) {
            log.warn("幂等校验: 未获取到requestId, 方法: {}", getMethodName(joinPoint));
            throw new BizException(ErrorCodeEnum.PARAM_INVALID, "写操作必须提供requestId");
        }

        String redisKey = IDEMPOTENT_KEY_PREFIX + UserContext.getUserId() + ":" + requestId;
        int expireSeconds = idempotent.expireSeconds();

        Boolean setSuccess = redisService.setIfAbsent(redisKey, "1", expireSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(setSuccess)) {
            log.warn("幂等校验: 重复请求被拦截, requestId: {}, 方法: {}", requestId, getMethodName(joinPoint));
            throw new BizException(ErrorCodeEnum.REQUEST_DUPLICATED,
                    "重复请求, requestId: " + requestId);
        }

        log.debug("幂等校验通过, requestId: {}, 方法: {}", requestId, getMethodName(joinPoint));
        return joinPoint.proceed();
    }

    /**
     * 从方法参数中反射提取requestId字段值
     * <p>
     * 依次扫描所有参数，返回第一个包含非空requestId字段的值。
     * 支持 @PathVariable + @RequestBody 组合的方法签名。
     * </p>
     *
     * @param joinPoint 切入点
     * @return requestId值，提取失败返回null
     */
    private String extractRequestId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }

        // 1. 先尝试从方法参数名中查找名为"requestId"的String参数
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = sig.getParameterNames();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                if ("requestId".equals(paramNames[i]) && args[i] instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        }

        // 2. 再尝试从对象参数反射提取requestId字段
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                Field field = findField(arg.getClass(), "requestId");
                if (field == null) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(arg);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            } catch (IllegalAccessException e) {
                log.error("幂等校验: 反射获取requestId失败, 参数类型: {}, 错误: {}",
                        arg.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        log.warn("幂等校验: 所有参数中均未找到有效requestId, 方法: {}", getMethodName(joinPoint));
        return null;
    }

    /**
     * 递归查找字段（包含父类）
     *
     * @param clazz     目标类
     * @param fieldName 字段名
     * @return Field对象，未找到返回null
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 获取方法全限定名（用于日志）
     *
     * @param joinPoint 切入点
     * @return 类名.方法名
     */
    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
