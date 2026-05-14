package com.lifechain.infra.config;

import lombok.Data;

/**
 * 系统配置视图对象
 */
@Data
public class SysConfigVO {
    private String configKey;
    private String configValue;
    private String configType;
    private String description;
    private String status;

    public static SysConfigVO fromEntity(SysConfigEntity entity) {
        SysConfigVO vo = new SysConfigVO();
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(entity.getConfigValue());
        vo.setConfigType(entity.getConfigType());
        vo.setDescription(entity.getDescription());
        vo.setStatus(entity.getStatus());
        return vo;
    }
}
