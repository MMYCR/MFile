package org.example.myselffile.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class RedisUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 🛡️ 防缓存穿透 & 防雪崩 获取数据
     * @param key 缓存 Key
     * @param clazz 返回类型
     * @param dbQuery 数据库/S3查询函数 (Lambda)
     * @param expireSeconds 过期时间
     * @return 结果对象
     */
    public <T> T get(String key, Class<T> clazz, Supplier<T> dbQuery, long expireSeconds) {
        String json = redisTemplate.opsForValue().get(key);

        if (json != null) {
            // 🛡 防穿透：如果是空对象标记 "{}"，直接返回 null
            if ("{}".equals(json)) {
                return null;
            }
            try {
                return mapper.readValue(json, clazz);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        //  Redis 没有，查数据库
        T result = dbQuery.get();

        if (result != null) {
            try {
                String value = mapper.writeValueAsString(result);
                //  防雪崩：给过期时间加一个随机值 (0~10%)
                long randomJitter = (long) (Math.random() * (expireSeconds * 0.1));
                redisTemplate.opsForValue().set(key, value, expireSeconds + randomJitter, TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } else {
            //  防穿透：数据库也没有，写入空对象 "{}"
            redisTemplate.opsForValue().set(key, "{}", 60, TimeUnit.SECONDS);
        }

        return result;
    }

    public <T> T getList(String key, Class<?> contentClass, Supplier<T> dbQuery, long expireSeconds) {
        return null;
    }

    // 删除缓存
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}