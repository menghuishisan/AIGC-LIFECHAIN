package com.lifechain.app.config;

import com.lifechain.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 注册全局拦截器（UserContext清理）和CORS跨域映射。
 * </p>
 *
 * @author LifeChain
 */
@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 注册拦截器
     * <p>
     * 注册UserContext清理拦截器，确保每次请求完成后清除ThreadLocal中的用户信息，
     * 防止线程复用导致的内存泄漏和数据串用。
     * </p>
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextCleanupInterceptor())
                .addPathPatterns("/**");
    }

    /**
     * UserContext 清理拦截器
     * <p>
     * 在请求处理完成后（无论成功或异常）清除ThreadLocal中的用户上下文，
     * 防止线程池复用导致的数据泄漏。
     * </p>
     */
    private static class UserContextCleanupInterceptor implements HandlerInterceptor {

        @Override
        public void afterCompletion(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull Object handler,
                                    Exception ex) {
            UserContext.clear();
        }
    }
}
