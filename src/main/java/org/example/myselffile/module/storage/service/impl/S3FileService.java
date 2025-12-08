package org.example.myselffile.module.storage.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.myselffile.module.model.FileNode;
import org.example.myselffile.module.model.dto.UploadInfo;
import org.example.myselffile.module.storage.model.param.S3Param;
import org.example.myselffile.module.storage.service.base.StorageStrategy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service("s3FileService")
@Scope("prototype")
public class S3FileService implements StorageStrategy {

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private String bucketName;
    private String endpoint;
    private String accessKey;
    private String secretKey;

    @Override
    public String getStorageType() {
        return "S3";
    }

    @Override
    public void init(String configJson) {
        try {
            if (configJson == null || configJson.trim().isEmpty()) {
                throw new IllegalArgumentException("S3配置为空");
            }

            ObjectMapper mapper = new ObjectMapper();
            // 忽略 JSON 中存在但 Java 类中不存在的字段
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            S3Param param = mapper.readValue(configJson, S3Param.class);

            this.bucketName = param.getBucketName();
            this.accessKey = param.getAccessKey();
            this.secretKey = param.getSecretKey();

            String rawEndpoint = param.getEndpoint();
            if (rawEndpoint == null) throw new RuntimeException("Endpoint 不能为空");

            // 去除首尾空格
            rawEndpoint = rawEndpoint.trim();

            //  自动补全 https://
            if (!rawEndpoint.startsWith("http://") && !rawEndpoint.startsWith("https://")) {
                rawEndpoint = "https://" + rawEndpoint;
            }
            this.endpoint = rawEndpoint;

            // 4. 创建 URI
            URI endpointUri = URI.create(rawEndpoint);

            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

            this.s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .endpointOverride(endpointUri)
                    .region(Region.US_EAST_1)
                    .forcePathStyle(false)    //  阿里云必须为 false
                    .build();

            this.s3Presigner = S3Presigner.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .endpointOverride(endpointUri)
                    .region(Region.US_EAST_1)
                    .build();

        } catch (Exception e) {
            e.printStackTrace(); // 打印报错
            throw new RuntimeException("S3初始化失败: " + e.getMessage());
        }
    }

    @Override
    public String getDownloadUrl(String path) {
        String key = formatKey(path);
        // 生成签名 URL，1小时有效，直连 OSS
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(key).build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public UploadInfo getUploadInfo(String path, String fileName) {
        String prefix = path;
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1); // 去掉开头的 /
        }
        // 如果路径不是空的（不是根目录），且不以 / 结尾，补上 /
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }
        String objectKey = prefix + fileName;

        // 1. 生成 Policy (限制上传大小 1GB，有效时间 1小时)
        String expiration = Instant.now().plus(Duration.ofHours(1)).toString();
        String policyJson = String.format("{\"expiration\": \"%s\",\"conditions\": [[\"content-length-range\", 0, 1073741824],[\"starts-with\", \"$key\", \"%s\"]]}",
                expiration, objectKey);

        String encodedPolicy = Base64.getEncoder().encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));

        // 2. 计算签名
        String signature = calculateSignature(encodedPolicy, this.secretKey);

        // 3. 构造返回给前端的参数
        Map<String, String> formData = new HashMap<>();
        formData.put("key", objectKey);
        formData.put("policy", encodedPolicy);
        formData.put("OSSAccessKeyId", this.accessKey);
        formData.put("Signature", signature);

        // 构造阿里云上传地址
        String postUrl = this.endpoint.replace("://", "://" + bucketName + ".");

        return UploadInfo.builder()
                .uploadType("DIRECT")
                .postUrl(postUrl)
                .formData(formData)
                .build();
    }

    @Override
    public boolean uploadProxy(String path, MultipartFile file) {
        // S3 模式不支持代理上传，前端应该走 DIRECT 模式
        return false;
    }


    @Override
    public List<FileNode> listFiles(String path) {
        String prefix = formatKey(path);
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";

        ListObjectsV2Request req = ListObjectsV2Request.builder()
                .bucket(bucketName).prefix(prefix).delimiter("/").build();
        ListObjectsV2Response res = s3Client.listObjectsV2(req);

        List<FileNode> list = new ArrayList<>();

        for (CommonPrefix cp : res.commonPrefixes()) {
            FileNode node = new FileNode();
            String dirName = cp.prefix();
            String[] parts = dirName.split("/");
            node.setName(parts[parts.length - 1]);
            node.setPath("/" + dirName);
            node.setType("FOLDER");
            list.add(node);
        }

        for (S3Object obj : res.contents()) {
            if (obj.key().equals(prefix)) continue;
            FileNode node = new FileNode();
            String[] parts = obj.key().split("/");
            node.setName(parts[parts.length - 1]);
            node.setPath("/" + obj.key());
            node.setSize(obj.size());
            node.setTime(Date.from(obj.lastModified()));
            node.setType("FILE");
            // 使用新接口获取链接
            node.setUrl(getDownloadUrl(node.getPath()));
            list.add(node);
        }
        return list;
    }

    @Override
    public ResponseEntity<Resource> getFileResource(String path) {
        // S3 不应该走流下载，直接重定向到签名链接
        try {
            return ResponseEntity.status(302).location(URI.create(getDownloadUrl(path))).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean createFolder(String path, String name) {
        String prefix = path;
        if (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }
        //  拼接新文件夹 Key (前面操作保证以 / 结尾，不以 / 开头,表示文件夹)
        String key = prefix + name + "/";

        try {
            // 4. 上传一个 0 字节的空对象，以 / 结尾，OSS 会自动识别为文件夹
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build(), RequestBody.empty());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("OSS创建文件夹失败: " + e.getMessage());
        }
    }

    @Override
    public boolean delete(String path) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(formatKey(path)).build());
        return true;
    }

    @Override
    public boolean rename(String path, String name, String newName) {
        String prefix = formatKey(path);
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }

        String sourceKey = prefix + name;
        String destinationKey = prefix + newName;

        try {
            //  复制对象
            String copySource = bucketName + "/" + sourceKey;
            copySource = java.net.URLEncoder.encode(copySource, StandardCharsets.UTF_8.toString());

            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();
            CopyObjectRequest fixedCopyReq = CopyObjectRequest.builder()
                    .copySource(bucketName + "/" + sourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(fixedCopyReq);

            //  删除旧对象
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(sourceKey)
                    .build());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("OSS重命名失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getPassword(String path) {
        try {
            String key = formatKey(path + "/password.txt");
            byte[] bytes = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build()).readAllBytes();
            return new String(bytes).trim();
        } catch (Exception e) {
            return null;
        }
    }

    // --- 工具方法 ---

    private String formatKey(String path) {
        if (path.startsWith("/")) return path.substring(1);
        return path;
    }

    private String calculateSignature(String data, String key) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("签名计算失败", e);
        }
    }
}