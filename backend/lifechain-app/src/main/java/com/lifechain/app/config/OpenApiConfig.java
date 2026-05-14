package com.lifechain.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 文档配置
 * <p>
 * 定义API文档的基本信息、认证方案和按模块分组展示。
 * 访问地址：/swagger-ui.html
 * </p>
 *
 * @author LifeChain
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AIGC内容可信管理平台 API",
                version = "1.0.0",
                description = "基于区块链的AIGC内容确权、交易与监管平台接口文档",
                contact = @Contact(name = "LifeChain Team")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT Bearer Token认证，格式：Bearer {token}"
)
public class OpenApiConfig {

    /**
     * 认证模块 API 分组
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("01-认证授权")
                .pathsToMatch("/api/auth/**")
                .build();
    }

    /**
     * 作品模块 API 分组
     */
    @Bean
    public GroupedOpenApi workApi() {
        return GroupedOpenApi.builder()
                .group("02-作品管理")
                .pathsToMatch("/api/works/**", "/api/claims/**", "/api/certificates/**")
                .build();
    }

    /**
     * 交易模块 API 分组
     */
    @Bean
    public GroupedOpenApi tradeApi() {
        return GroupedOpenApi.builder()
                .group("03-交易市场")
                .pathsToMatch("/api/market/**", "/api/orders/**", "/api/payments/**", "/api/licenses/**")
                .build();
    }

    /**
     * 结算模块 API 分组
     */
    @Bean
    public GroupedOpenApi settlementApi() {
        return GroupedOpenApi.builder()
                .group("04-结算中心")
                .pathsToMatch("/api/settlements/**")
                .build();
    }

    /**
     * 监管模块 API 分组
     */
    @Bean
    public GroupedOpenApi regulatorApi() {
        return GroupedOpenApi.builder()
                .group("05-监管合规")
                .pathsToMatch("/api/regulator/**")
                .build();
    }

    /**
     * 管理后台 API 分组
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("06-管理后台")
                .pathsToMatch("/api/admin/**")
                .build();
    }

    /**
     * 公共接口 API 分组
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("07-公开接口")
                .pathsToMatch("/api/public/**", "/api/trace/**")
                .build();
    }
}
