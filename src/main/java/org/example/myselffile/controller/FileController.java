package org.example.myselffile.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.myselffile.module.model.FileNode;
import org.example.myselffile.module.model.dto.UploadInfo; // 🟢 记得导入这个新类
import org.example.myselffile.module.model.result.AjaxJson;
import org.example.myselffile.module.storage.context.StorageSourceContext;
import org.example.myselffile.module.storage.mapper.StorageSourceMapper;
import org.example.myselffile.module.storage.model.entity.StorageSource;
import org.example.myselffile.module.storage.service.base.StorageStrategy; // 🟢 改用新接口
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FileController {

    @Autowired
    private StorageSourceContext storageSourceContext;

    @Autowired
    private StorageSourceMapper storageSourceMapper;

    /**
     * 1. 获取文件列表
     */
    @GetMapping("/list")
    public AjaxJson<List<FileNode>> list(@RequestParam(required = false) String storageKey,
                                         @RequestParam(defaultValue = "/") String path,
                                         @RequestParam(required = false) String password) {

        // 1. 获取策略实现类 (LocalStrategy 或 S3FileService)
        StorageStrategy strategy = storageSourceContext.getService(storageKey);

        // 2.  密码校验逻辑
        String expectedPwd = strategy.getPassword(path);
        if (expectedPwd != null && !expectedPwd.isEmpty()) {
            if (password == null || !password.equals(expectedPwd)) {
                return AjaxJson.getError(403, "该文件夹需要密码");
            }
        }

        List<FileNode> files = strategy.listFiles(path);

        if (files != null) {
            files.removeIf(f -> f.getName().equals("password.txt"));
        }

        return AjaxJson.getSuccessData(files);
    }

    /**
     * 2. 获取存储源列表 (侧边栏用)
     */
    @GetMapping("/storage/list")
    public AjaxJson<List<StorageSource>> publicList() {
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
     * 前端上传前必调此接口。
     * - 如果是 Local，返回 uploadType="PROXY" 和后端接口地址。
     * - 如果是 S3，返回 uploadType="DIRECT" 和阿里云签名、上传地址。
     */
    @GetMapping("/upload/info")
    public AjaxJson<UploadInfo> getUploadInfo(@RequestParam String storageKey,
                                              @RequestParam String path,
                                              @RequestParam String fileName) {
        StorageStrategy strategy = storageSourceContext.getService(storageKey);
        return AjaxJson.getSuccessData(strategy.getUploadInfo(path, fileName));
    }

    /**
     * 代理上传
     * 只有当 /upload/info 返回 PROXY 模式时，前端才会调这个接口。
     * 方法名改为调用 uploadProxy
     */
    @PostMapping("/upload")
    public AjaxJson<Void> upload(@RequestParam(required = false) String storageKey,
                         @RequestParam String path,
                         @RequestParam MultipartFile file) {
        StorageStrategy strategy = storageSourceContext.getService(storageKey);
        boolean result = strategy.uploadProxy(path, file);
        return result ? AjaxJson.getSuccess("上传成功") : AjaxJson.getError("上传失败");
    }

    /**
     * 获取下载链接
     * 用于前端预览、播放视频、以及分享功能。
     * - Local: 返回 /api/download?path=...
     * - S3: 返回 https://bucket.oss... (带签名的直链)
     */
    @GetMapping("/download/url")
    public AjaxJson<String> getDownloadUrl(@RequestParam String storageKey,
                                           @RequestParam String path) {
        StorageStrategy strategy = storageSourceContext.getService(storageKey);
        return AjaxJson.getSuccessData(strategy.getDownloadUrl(path));
    }

    /**
     * 6. 后端流式下载
     * 仅当存储源为 Local 时，前端通过 getDownloadUrl 拿到这个接口的地址来下载。
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam(required = false) String storageKey,
                                             @RequestParam String path) {
        return storageSourceContext.getService(storageKey).getFileResource(path);
    }


    @PostMapping("/mkdir")
    public String mkdir(@RequestParam(required = false) String storageKey,
                        @RequestParam String path, @RequestParam String name) {
        boolean result = storageSourceContext.getService(storageKey).createFolder(path, name);
        return result ? "success" : "fail";
    }

    @DeleteMapping("/delete")
    public String delete(@RequestParam(required = false) String storageKey,
                         @RequestParam String path) {
        boolean result = storageSourceContext.getService(storageKey).delete(path);
        return result ? "success" : "fail";
    }

    @PostMapping("/rename")
    public AjaxJson<Void> rename(@RequestParam(required = false) String storageKey,
                                 @RequestParam String path,
                                 @RequestParam String name,
                                 @RequestParam String newName) {
        boolean success = storageSourceContext.getService(storageKey).rename(path, name, newName);
        return success ? AjaxJson.getSuccess("重命名成功") : AjaxJson.getError("重命名失败");
    }
}