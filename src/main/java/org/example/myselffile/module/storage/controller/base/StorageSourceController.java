package org.example.myselffile.module.storage.controller.base;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.myselffile.module.model.result.AjaxJson;
import org.example.myselffile.module.storage.mapper.StorageSourceMapper;
import org.example.myselffile.module.storage.model.entity.StorageSource;
import org.example.myselffile.module.storage.model.enums.StorageTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/storage")
public class StorageSourceController {

    @Autowired
    private StorageSourceMapper storageSourceMapper;

    // 1. 获取所有存储源列表
    @GetMapping("/list")
    public AjaxJson<List<StorageSource>> list() {
        long currentUserId = StpUtil.getLoginIdAsLong(); // 获取当前登录人

        List<StorageSource> list = storageSourceMapper.selectList(
                new LambdaQueryWrapper<StorageSource>()
                        .eq(StorageSource::getUserId, currentUserId) // 🟢 关键：只查自己的
                        .orderByAsc(StorageSource::getOrderNum)
        );
        return AjaxJson.getSuccessData(list);
    }

    // 2. 保存/修改存储源
    @PostMapping("/save")
    public AjaxJson<Void> save(@RequestBody StorageSource storageSource) {
        long currentUserId = StpUtil.getLoginIdAsLong();

        if (storageSource.getId() == null) {
            storageSource.setUserId(currentUserId);
            storageSource.setEnable(true);

            if (storageSource.getType() == null) {
                storageSource.setType(StorageTypeEnum.LOCAL);
            }

            storageSourceMapper.insert(storageSource);
        } else {
            StorageSource exist = storageSourceMapper.selectById(storageSource.getId());
            if (exist == null || !exist.getUserId().equals(currentUserId)) {
                return AjaxJson.getError("无权操作此存储源");
            }
            storageSource.setUserId(currentUserId);

            storageSourceMapper.updateById(storageSource);
        }
        return AjaxJson.getSuccess("保存成功");
    }

    // 3. 删除
    @DeleteMapping("/delete/{id}")
    public AjaxJson<Void> delete(@PathVariable Long id) {
        long currentUserId = StpUtil.getLoginIdAsLong();
        StorageSource exist = storageSourceMapper.selectById(id);

        if (exist != null) {
            if (!exist.getUserId().equals(currentUserId)) {
                return AjaxJson.getError("无权删除此存储源");
            }
            storageSourceMapper.deleteById(id);
        }
        return AjaxJson.getSuccess("删除成功");
    }

    // 4. 获取单个详情
    @GetMapping("/info/{id}")
    public AjaxJson<StorageSource> info(@PathVariable Long id) {
        return AjaxJson.getSuccessData(storageSourceMapper.selectById(id));
    }

}