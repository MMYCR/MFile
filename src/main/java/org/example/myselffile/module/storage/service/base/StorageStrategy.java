package org.example.myselffile.module.storage.service.base;

import org.example.myselffile.module.model.FileNode;
import org.example.myselffile.module.model.dto.UploadInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface StorageStrategy {

    /**
     * 获取存储源类型 (例如 LOCAL, S3, ALIYUN)
     */
    String getStorageType();

    /**
     * 初始化配置
     * @param configData 对于Local是路径字符串，对于OSS是JSON配置
     */
    void init(String configData);

    /**
     * 列出文件
     */
    List<FileNode> listFiles(String path);

    /**
     * 获取本地文件资源 (用于下载)
     * 仅当 getDownloadUrl 返回的是后端接口时，Controller 才会调这个方法
     */
    ResponseEntity<Resource> getFileResource(String path);

    /**
     * 新建文件夹
     */
    boolean createFolder(String path, String name);

    /**
     * 删除
     */
    boolean delete(String path);

    /**
     * 上传
     * 只有当 getUploadInfo 返回 PROXY 模式时，前端才会调这个接口
     */
    boolean uploadProxy(String path, MultipartFile file);

    /**
     * 获取指定路径下的密码（读取 password.txt）
     * @param path 文件夹路径
     * @return 密码字符串，如果没有则返回 null
     */
    String getPassword(String path);

    /**
     * 重命名文件
     * @param path 当前文件夹路径 (如 /photos)
     * @param name 旧文件名 (如 1.jpg)
     * @param newName 新文件名 (如 2.jpg)
     */
    boolean rename(String path, String name, String newName);

    /**
     * 获取下载链接
     * 用于分享功能和前端预览。
     * Local: 返回后端接口地址 "/api/proxy/download?path=..."
     * OSS: 返回阿里云签名URL "https://bucket.oss..."
     */
    String getDownloadUrl(String path);

    /**
     * 获取上传配置 (前端第一步)
     * 告诉前端应该往哪里传
     */
    UploadInfo getUploadInfo(String path, String fileName);
}