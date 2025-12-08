package org.example.myselffile.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import org.example.myselffile.module.model.result.AjaxJson;
import org.example.myselffile.module.storage.context.StorageSourceContext;
import org.example.myselffile.module.storage.mapper.ShareLinkMapper;
import org.example.myselffile.module.storage.model.entity.ShareLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private ShareLinkMapper shareLinkMapper;

    @Autowired
    private StorageSourceContext context;

    //  创建分享链接
    @PostMapping("/create")
    public AjaxJson<String> createShare(@RequestParam String storageKey,
                                        @RequestParam String path,
                                        @RequestParam(defaultValue = "1") int days) { // 默认有效期1天

        ShareLink share = new ShareLink();
        share.setUuid(UUID.randomUUID().toString().replace("-", "").substring(0, 10)); // 生成10位短码
        share.setStorageKey(storageKey);
        share.setPath(path);
        // 设置过期时间
        share.setExpireTime(new Date(System.currentTimeMillis() + days * 24L * 60 * 60 * 1000));

        shareLinkMapper.insert(share);

        return AjaxJson.getSuccessData("http://localhost:5173/s/" + share.getUuid());
    }

    //  获取分享文件的下载链接
    @SaIgnore
    @GetMapping("/download/{uuid}")
    public AjaxJson<String> downloadShare(@PathVariable String uuid) {
        // 1. 查库
        ShareLink share = shareLinkMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getUuid, uuid)
        );

        if (share == null) return AjaxJson.getError("链接不存在");
        if (share.getExpireTime().before(new Date())) return AjaxJson.getError("链接已过期");

        // 2. 调用策略模式获取下载链 (Local/OSS)
        String downloadUrl = context.getService(share.getStorageKey())
                .getDownloadUrl(share.getPath());

        return AjaxJson.getSuccessData(downloadUrl);
    }
}