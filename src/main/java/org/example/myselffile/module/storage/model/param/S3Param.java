package org.example.myselffile.module.storage.model.param;

import lombok.Data;

@Data
public class S3Param {
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String bucketName;
    // 兼容前端可能传过来的其他字段，避免 Jackson 报错
    private String region;
}