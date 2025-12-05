package org.example.myselffile.module.storage.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.myselffile.module.storage.model.enums.StorageTypeEnum;

import java.io.Serializable;

@Data
@TableName("storage_source")
public class StorageSource implements Serializable {
    @TableId(value="id",type = IdType.AUTO)
    private Long id;

    @TableField(value = "enable")
    private Boolean enable;

    @TableField(value = "`name`")
    private String name;

    @TableField(value = "`key`")
    private String key;

    @TableField(value = "`type`")
    private StorageTypeEnum type;

    @TableField(value = "order_num")
    private Integer orderNum;

    @TableField(value = "root_path")
    private String rootPath;

    @TableField("config_data")
    private String configData;

    @TableField("user_id")
    private Long userId;
}