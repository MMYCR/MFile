package org.example.myselffile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.myselffile.module.model.FileNode;
import org.example.myselffile.module.model.dto.UploadInfo;
import org.example.myselffile.module.model.result.AjaxJson;
import org.example.myselffile.module.storage.context.StorageSourceContext;
import org.example.myselffile.module.storage.mapper.StorageSourceMapper;
import org.example.myselffile.module.storage.model.entity.StorageSource;
import org.example.myselffile.module.storage.service.base.StorageStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private StorageSourceContext storageSourceContext;

    @Autowired
    private StorageSourceMapper storageSourceMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Jackson 工具，用于 List<FileNode> 和 String 之间的转换
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 1. 获取文件列表 (核心高并发接口)
     * 策略：Redis 缓存 + 随机过期时间 (防雪崩)
     */
    @GetMapping("/list")
    public AjaxJson<List<FileNode>> list(@RequestParam(required = false) String storageKey,
                                         @RequestParam(defaultValue = "/") String path,
                                         @RequestParam(required = false) String password) {

        //  获取策略实现类
        StorageStrategy strategy = storageSourceContext.getService(storageKey);
        //  密码校验逻辑 (安全校验不走缓存，必须实时)
        String expectedPwd = strategy.getPassword(path);
        if (expectedPwd != null && !expectedPwd.isEmpty()) {
            if (password == null || !password.equals(expectedPwd)) {
                return AjaxJson.getError(403, "该文件夹需要密码");
            }
        }
        //  构造缓存 Key
        String cacheKey = "files:" + (storageKey == null ? "default" : storageKey) + ":" + path;

        //  先查 Redis
        String jsonResult = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(jsonResult)) {
            try {
                // 反序列化 JSON -> List<FileNode>
                List<FileNode> cachedFiles = mapper.readValue(jsonResult, new TypeReference<List<FileNode>>() {});
                return AjaxJson.getSuccessData(cachedFiles);
            } catch (Exception e) {
                // 如果缓存数据损坏，忽略错误，降级查源端
                redisTemplate.delete(cacheKey);
            }
        }

        // 缓存未命中查询真实存储
        List<FileNode> files = strategy.listFiles(path);

        if (files != null) {
            // 过滤密码文件
            files.removeIf(f -> f.getName().equals("password.txt"));

            // 写入 Redis (防雪崩：随机过期时间)
            try {
                String jsonStr = mapper.writeValueAsString(files);
                // 基础过期时间 60秒 + 随机抖动 0~10秒
                long expire = 60 + (long) (Math.random() * 10);
                redisTemplate.opsForValue().set(cacheKey, jsonStr, expire, TimeUnit.SECONDS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return AjaxJson.getSuccessData(files);
    }

    /**
     * 2. 获取存储源列表
     */
    @GetMapping("/storage/list")
    public AjaxJson<List<StorageSource>> publicList() {
        //数据量小且用户绑定,不做缓存
        long currentUserId = StpUtil.getLoginIdAsLong();
        List<StorageSource> list = storageSourceMapper.selectList(
                new LambdaQueryWrapper<StorageSource>()
                        .eq(StorageSource::getEnable, true)
                        .eq(StorageSource::getUserId, currentUserId)
                        .orderByAsc(StorageSource::getOrderNum)
        );
        return AjaxJson.getSuccessData(list);
    }

    /**
     * 获取上传配置
     */
    @GetMapping("/upload/info")
    public AjaxJson<UploadInfo> getUploadInfo(@RequestParam String storageKey,
                                              @RequestParam String path,
                                              @RequestParam String fileName) {
        StorageStrategy strategy = storageSourceContext.getService(storageKey);
        return AjaxJson.getSuccessData(strategy.getUploadInfo(path, fileName));
    }

    /**
     * 代理上传 (写操作)
     */
    @PostMapping("/upload")
    public AjaxJson<Void> upload(@RequestParam(required = false) String storageKey,
                                 @RequestParam String path,
                                 @RequestParam MultipartFile file) {
        StorageStrategy strategy = storageSourceContext.getService(storageKey);
        boolean result = strategy.uploadProxy(path, file);

        if (result) {
            // 写成功后，删除当前目录的缓存，保证下次读取是新的
            clearCache(storageKey, path);
            return AjaxJson.getSuccess("上传成功");
        }
        return AjaxJson.getError("上传失败");
    }

    @GetMapping("/download/url")
    public AjaxJson<String> getDownloadUrl(@RequestParam String storageKey,
                                           @RequestParam String path) {
        StorageStrategy strategy = storageSourceContext.getService(storageKey);
        return AjaxJson.getSuccessData(strategy.getDownloadUrl(path));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam(required = false) String storageKey,
                                             @RequestParam String path) {
        return storageSourceContext.getService(storageKey).getFileResource(path);
    }

    /**
     * 新建文件夹 (写操作)
     */
    @PostMapping("/mkdir")
    public AjaxJson<Void> mkdir(@RequestParam(required = false) String storageKey,
                        @RequestParam String path, @RequestParam String name) {
        boolean result = storageSourceContext.getService(storageKey).createFolder(path, name);
        if (result) {
            clearCache(storageKey, path);
            return AjaxJson.getSuccess("创建成功");
        }
        return AjaxJson.getError("创建失败");
    }

    /**
     * 删除文件/文件夹 (写操作)
     */
    @DeleteMapping("/delete")
    public AjaxJson<Void> delete(@RequestParam(required = false) String storageKey,
                         @RequestParam String path) {
        boolean result = storageSourceContext.getService(storageKey).delete(path);
        if (result) {
            // 清除父目录缓存
            String parentPath = getParentPath(path);
            clearCache(storageKey, parentPath);
            return AjaxJson.getSuccess("删除成功");
        }
        return AjaxJson.getError("删除失败");
    }

    /**
     * 重命名 (写操作)
     */
    @PostMapping("/rename")
    public AjaxJson<Void> rename(@RequestParam(required = false) String storageKey,
                                 @RequestParam String path,
                                 @RequestParam String name,
                                 @RequestParam String newName) {
        boolean success = storageSourceContext.getService(storageKey).rename(path, name, newName);
        if (success) {
            // 清除父目录缓存
            clearCache(storageKey, path);
            return AjaxJson.getSuccess("重命名成功");
        }
        return AjaxJson.getError("重命名失败");
    }

    // --- 辅助方法 ---

    /**
     * 清除 Redis 缓存
     */
    private void clearCache(String storageKey, String path) {
        String cacheKey = "files:" + (storageKey == null ? "default" : storageKey) + ":" + path;
        redisTemplate.delete(cacheKey);
    }

    /**
     * 获取父目录路径 (用于 delete 操作清除缓存)
     * /movies/test.mp4 -> /movies
     */
    private String getParentPath(String path) {
        if (path == null || path.equals("/")) return "/";
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int lastSlash = path.lastIndexOf("/");
        if (lastSlash <= 0) return "/";
        return path.substring(0, lastSlash);
    }
}