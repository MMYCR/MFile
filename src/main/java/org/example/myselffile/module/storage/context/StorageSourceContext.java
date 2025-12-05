package org.example.myselffile.module.storage.context;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.myselffile.module.storage.mapper.StorageSourceMapper;
import org.example.myselffile.module.storage.model.entity.StorageSource;
import org.example.myselffile.module.storage.model.enums.StorageTypeEnum; // 导入你的枚举
import org.example.myselffile.module.storage.service.base.StorageStrategy;
import org.example.myselffile.module.storage.service.impl.LocalFileService;
import org.example.myselffile.module.storage.service.impl.S3FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class StorageSourceContext {

    @Autowired
    private StorageSourceMapper storageSourceMapper;

    // 这一步虽然现在没用上，但为了以后用 Spring 原型 Bean 获取 Service 最好保留
    @Autowired
    private ApplicationContext applicationContext;

    public StorageStrategy getService(String storageKey) {
        //  获取当前用户id
        long currentUserId = StpUtil.getLoginIdAsLong();

        // 1. 查数据库
        LambdaQueryWrapper<StorageSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorageSource::getEnable, true);
        wrapper.eq(StorageSource::getUserId, currentUserId);

        if (storageKey != null && !storageKey.isEmpty()) {
            wrapper.eq(StorageSource::getKey, storageKey);
        } else {
            wrapper.orderByAsc(StorageSource::getOrderNum).last("LIMIT 1");
        }

        StorageSource source = storageSourceMapper.selectOne(wrapper);
        if (source == null) {
            // 这里抛出的异常会被 Controller 捕获，显示给前端
            throw new RuntimeException("存储源不存在或无权访问");
        }

        StorageStrategy service;

        // 2. 根据类型选择 Service 并初始化
        // 🟢 核心修改：直接用枚举对象对比 (==)，不用 getCode() 也不用 getValue()
        if (StorageTypeEnum.LOCAL == source.getType()) {
            service = new LocalFileService();
            service.init(source.getRootPath());
        }
        else if (StorageTypeEnum.ALIYUN == source.getType()) {
            service = new S3FileService();
            // S3 需要解析 config_data (JSON)，而不是 root_path
            service.init(source.getConfigData());
        }
        else {
            throw new RuntimeException("暂不支持该存储类型");
        }

        return service;
    }
}