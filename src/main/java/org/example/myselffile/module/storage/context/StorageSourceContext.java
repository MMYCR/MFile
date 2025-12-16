package org.example.myselffile.module.storage.context;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.myselffile.module.storage.mapper.StorageSourceMapper;
import org.example.myselffile.module.storage.model.entity.StorageSource;
import org.example.myselffile.module.storage.model.enums.StorageTypeEnum;
import org.example.myselffile.module.storage.service.base.StorageStrategy;
import org.example.myselffile.module.storage.service.impl.LocalFileService;
import org.example.myselffile.module.storage.service.impl.S3FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class StorageSourceContext {

    @Autowired
    private StorageSourceMapper storageSourceMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper mapper = new ObjectMapper();

    public StorageStrategy getService(String storageKey) {
        long currentUserId = StpUtil.getLoginIdAsLong();

        //  构造缓存 Key： config:userId:storageKey
        String cacheKey = "config:" + currentUserId + ":" + (storageKey == null ? "default" : storageKey);

        StorageSource source = null;

        //  查缓存
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json != null) {
            try {
                if ("{}".equals(json)) throw new RuntimeException("存储源不存在"); // 防穿透结果
                source = mapper.readValue(json, StorageSource.class);
            } catch (Exception e) {
                // 忽略解析错误，走库
            }
        }

        //  缓存没命中，查数据库
        if (source == null) {
            LambdaQueryWrapper<StorageSource> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(StorageSource::getEnable, true);
            wrapper.eq(StorageSource::getUserId, currentUserId);
            if (storageKey != null && !storageKey.isEmpty()) {
                wrapper.eq(StorageSource::getKey, storageKey);
            } else {
                wrapper.orderByAsc(StorageSource::getOrderNum).last("LIMIT 1");
            }
            source = storageSourceMapper.selectOne(wrapper);

            //  写回缓存 (30分钟过期)
            try {
                if (source != null) {
                    redisTemplate.opsForValue().set(cacheKey, mapper.writeValueAsString(source), 30, TimeUnit.MINUTES);
                } else {
                    // 防穿透：存空值，1分钟过期
                    redisTemplate.opsForValue().set(cacheKey, "{}", 1, TimeUnit.MINUTES);
                }
            } catch (Exception e) {}
        }

        if (source == null) {
            throw new RuntimeException("存储源不存在或无权访问");
        }

        // 实例化 Service
        StorageStrategy service;
        if (StorageTypeEnum.LOCAL == source.getType()) {
            service = new LocalFileService();
            service.init(source.getRootPath());
        }
        else if (StorageTypeEnum.ALIYUN == source.getType()) {
            service = new S3FileService();
            service.init(source.getConfigData());
        }
        else {
            throw new RuntimeException("暂不支持该存储类型");
        }
        return service;
    }
}