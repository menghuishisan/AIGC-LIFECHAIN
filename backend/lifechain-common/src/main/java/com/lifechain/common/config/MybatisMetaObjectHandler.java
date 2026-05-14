package com.lifechain.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.lifechain.common.util.DateTimeUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 * <p>
 * 统一为 createdAt 和 updatedAt 字段填充UTC时间。
 * </p>
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = DateTimeUtil.nowUtc();
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // 默认未删除
        this.strictInsertFill(metaObject, "deletedFlag", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, DateTimeUtil.nowUtc());
    }
}
