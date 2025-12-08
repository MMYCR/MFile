package org.example.myselffile.module.storage.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum StorageTypeEnum{
    LOCAL("local", "本地存储"),
    ALIYUN("aliyun", "阿里云 OSS"),
    TENCENT("tencent", "腾讯云 COS");

    @JsonValue
    @EnumValue
    private final String code;
    private final String description;

    StorageTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据code获取对应的枚举
     * @param code 存储提供商的code
     * @return 对应的CloudStorageProvider枚举，如果不存在则返回null
     */
    public static StorageTypeEnum getByCode(String code) {
        for (StorageTypeEnum provider : values()) {
            if (provider.getCode().equals(code)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * 根据description获取对应的枚举
     * @param description 存储提供商的描述
     * @return 对应的CloudStorageProvider枚举，如果不存在则返回null
     */
    public static StorageTypeEnum getByDescription(String description) {
        for (StorageTypeEnum provider : values()) {
            if (provider.getDescription().equals(description)) {
                return provider;
            }
        }
        return null;
    }

    @JsonCreator
    public static StorageTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (StorageTypeEnum e : values()) {
            // 忽略大小写对比
            if (e.code.equalsIgnoreCase(code) || e.name().equalsIgnoreCase(code)) {
                return e;
            }
        }
        // 如果找不到，返回 null，或者抛出异常让 Spring 报错
        return null;
    }
}