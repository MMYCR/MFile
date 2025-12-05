package org.example.myselffile.module.storage.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("share_link")
public class ShareLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String uuid;
    private String storageKey;
    private String path;
    private Date expireTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}