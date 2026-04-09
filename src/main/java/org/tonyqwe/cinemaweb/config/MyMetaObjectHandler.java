package org.tonyqwe.cinemaweb.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis-Plus 元对象处理器，用于自动填充时间戳
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间
        this.strictInsertFill(metaObject, "createdAt", Date.class, new Date());
        // 填充更新时间
        this.strictInsertFill(metaObject, "updatedAt", Date.class, new Date());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updatedAt", Date.class, new Date());
    }
}
