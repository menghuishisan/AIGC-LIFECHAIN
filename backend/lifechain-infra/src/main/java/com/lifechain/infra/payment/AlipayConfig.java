package com.lifechain.infra.payment;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置
 * <p>
 * 读取 lifechain.pay.alipay 前缀的配置项，包含应用ID、商户私钥、支付宝公钥等。
 * 所有敏感配置建议通过环境变量或加密配置中心注入。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "lifechain.pay.alipay")
public class AlipayConfig {

    /** Skill已验证的沙箱JSON绝对路径；不依赖进程工作目录，不将密钥复制到.env。 */
    private String sandboxConfigPath;

    @jakarta.annotation.PostConstruct
    public void loadSandbox() {
        // 正式网关使用当前环境注入的同一套应用参数，遗留沙箱文件不得覆盖正式配置。
        if ("https://openapi.alipay.com/gateway.do".equals(serverUrl)) return;
        if (sandboxConfigPath == null || sandboxConfigPath.isBlank()) return;
        if (serverUrl != null && !serverUrl.isBlank()
                && !"https://openapi-sandbox.dl.alipaydev.com/gateway.do".equals(serverUrl)) {
            throw new IllegalStateException("支付宝网关必须为受支持的官方HTTPS网关");
        }
        try {
            var path = java.nio.file.Path.of(sandboxConfigPath);
            if (!path.isAbsolute() || java.nio.file.Files.isSymbolicLink(path)
                    || !java.nio.file.Files.isRegularFile(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException();
            }
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(path.toFile());
            var app = root.path("appIds").path(0);
            String loadedApp = requiredText(app, "appId");
            String loadedPrivate = requiredText(app, "appPrivateKey");
            String loadedPublic = requiredText(app, "alipayPublicKey");
            String loadedSeller = requiredText(app, "pid");
            if (!loadedSeller.equals(requiredText(root.path("sandboxAccounts").path("partner"), "userId"))) {
                throw new IllegalArgumentException();
            }
            // 先完整验证，再一次映射同一应用；不允许与其他环境的应用或商户字段混用。
            appId = loadedApp;
            privateKey = loadedPrivate;
            alipayPublicKey = loadedPublic;
            sellerId = loadedSeller;
            serverUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
        } catch (Exception e) {
            // 不把解析器异常、文件内容或密钥写入日志。
            throw new IllegalStateException("支付宝沙箱配置缺失或无效，请先通过Skill验证同一配置文件");
        }
    }

    private static String requiredText(com.fasterxml.jackson.databind.JsonNode node, String field) {
        var value = node.path(field);
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().contains("<")) {
            throw new IllegalArgumentException();
        }
        return value.textValue();
    }

    /** 支付宝应用ID */
    private String appId;

    /** 商户RSA私钥 */
    @lombok.ToString.Exclude
    private String privateKey;

    /** 支付宝RSA公钥 */
    @lombok.ToString.Exclude
    private String alipayPublicKey;

    /** 收款商户 PID，回调必须与此值匹配 */
    private String sellerId;

    /** 浏览器支付结束后的结果页，仅用于展示，不能作为支付成功依据 */
    private String returnUrl;

    /** 支付宝网关地址 */
    private String serverUrl;

    /** 支付结果通知URL */
    private String notifyUrl;

}
