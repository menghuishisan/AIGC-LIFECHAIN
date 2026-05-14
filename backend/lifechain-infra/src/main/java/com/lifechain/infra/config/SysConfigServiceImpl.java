package com.lifechain.infra.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;

    @Override
    public String getConfigValue(String configKey) {
        return getConfigValue(configKey, null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        SysConfigEntity entity = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfigEntity>()
                        .eq(SysConfigEntity::getConfigKey, configKey)
                        .eq(SysConfigEntity::getStatus, "ACTIVE"));
        return entity != null ? entity.getConfigValue() : defaultValue;
    }

    @Override
    public List<SysConfigVO> listByType(String configType) {
        return configMapper.selectList(
                new LambdaQueryWrapper<SysConfigEntity>()
                        .eq(SysConfigEntity::getConfigType, configType)
                        .eq(SysConfigEntity::getStatus, "ACTIVE")
                        .orderByAsc(SysConfigEntity::getConfigKey))
                .stream().map(SysConfigVO::fromEntity).toList();
    }

    @Override
    public List<SysConfigVO> listAll() {
        return configMapper.selectList(
                new LambdaQueryWrapper<SysConfigEntity>()
                        .eq(SysConfigEntity::getStatus, "ACTIVE")
                        .orderByAsc(SysConfigEntity::getConfigType, SysConfigEntity::getConfigKey))
                .stream().map(SysConfigVO::fromEntity).toList();
    }

    @Override
    public SysConfigVO upsertConfig(String configKey, String configValue,
                                    String configType, String description) {
        SysConfigEntity existing = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfigEntity>()
                        .eq(SysConfigEntity::getConfigKey, configKey));
        if (existing != null) {
            existing.setConfigValue(configValue);
            if (configType != null) existing.setConfigType(configType);
            if (description != null) existing.setDescription(description);
            existing.setStatus("ACTIVE");
            configMapper.updateById(existing);
            log.info("配置更新成功, configKey={}", configKey);
            return SysConfigVO.fromEntity(existing);
        } else {
            SysConfigEntity entity = new SysConfigEntity();
            entity.setConfigKey(configKey);
            entity.setConfigValue(configValue);
            entity.setConfigType(configType != null ? configType : "GENERAL");
            entity.setDescription(description);
            entity.setStatus("ACTIVE");
            configMapper.insert(entity);
            log.info("配置创建成功, configKey={}", configKey);
            return SysConfigVO.fromEntity(entity);
        }
    }

    @Override
    public void deleteConfig(String configKey) {
        SysConfigEntity entity = configMapper.selectOne(
                new LambdaQueryWrapper<SysConfigEntity>()
                        .eq(SysConfigEntity::getConfigKey, configKey));
        if (entity != null) {
            configMapper.deleteById(entity.getId());
            log.info("配置删除(软删), configKey={}", configKey);
        }
    }
}
