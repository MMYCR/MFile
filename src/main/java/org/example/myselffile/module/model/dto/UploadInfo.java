package org.example.myselffile.module.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class UploadInfo {
    /**
     * 上传模式：
     * "PROXY"  -> 走后端代理 (Local)
     * "DIRECT" -> 前端直传 (Aliyun OSS)
     */
    private String uploadType;

    /**
     * 上传的目标 URL
     * PROXY 模式下： /api/upload
     * DIRECT 模式下： oss-cn-shenzhen.aliyuncs.com
     */
    private String postUrl;

    /**
     * 需要携带的额外表单参数
     * OSS 直传时需要携带：policy, signature, OSSAccessKeyId, key 等
     */
    private Map<String, String> formData;
}