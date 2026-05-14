package com.lifechain.common.annotation;

import java.lang.annotation.*;

/**
 * 幂等请求注解
 * <p>
 * 标注在写接口的Controller方法上，通过requestId实现幂等。
 * 幂等逻辑由公共切面统一处理，不需要各模块自行判重。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键在请求参数中的SpEL表达式
     * 默认取请求体中的 requestId 字段
     */
    String key() default "#request.requestId";

    /**
     * 幂等窗口时间（秒）
     */
    int expireSeconds() default 3600;
}
