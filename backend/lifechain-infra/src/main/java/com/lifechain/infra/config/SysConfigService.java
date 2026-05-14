package com.lifechain.infra.config;

import java.util.List;

/**
 * 系统配置服务接口
 */
public interface SysConfigService {

    /**
     * 获取配置值
     */
    String getConfigValue(String configKey);

    /**
     * 获取配置值，带默认值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 按类型查询配置列表
     */
    List<SysConfigVO> listByType(String configType);

    /**
     * 查询所有活跃配置
     */
    List<SysConfigVO> listAll();

    /**
     * 创建或更新配置
     */
    SysConfigVO upsertConfig(String configKey, String configValue,
                             String configType, String description);

    /**
     * 删除配置（软删除）
     */
    void deleteConfig(String configKey);
}
