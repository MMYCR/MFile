package org.example.myselffile.module.storage.service.impl;

import org.example.myselffile.module.model.FileNode;
import org.example.myselffile.module.model.dto.UploadInfo;
import org.example.myselffile.module.storage.service.base.StorageStrategy;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("localFileService")
@Scope("prototype")
public class LocalFileService implements StorageStrategy {

    private String rootPath;

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    @Override
    public void init(String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public List<FileNode> listFiles(String relativePath) {
        File dir = new File(this.rootPath, relativePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }

        File[] files = dir.listFiles();
        List<FileNode> result = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                FileNode node = new FileNode();
                node.setName(file.getName());
                node.setSize(file.length());
                node.setTime(new Date(file.lastModified()));
                String currentPath = relativePath.equals("/") ? "" : relativePath;
                node.setPath(currentPath + "/" + file.getName());

                if (file.isDirectory()) {
                    node.setType("FOLDER");
                } else {
                    node.setType("FILE");
                    // ⚠️ 注意：这里不直接拼接 URL 了，而是留给前端调用 getDownloadUrl
                    // 或者你也可以为了兼容旧前端先保留
                    node.setUrl("/api/download?path=" + node.getPath());
                }
                result.add(node);
            }
        }
        return result;
    }

    @Override
    public String getDownloadUrl(String path) {
        try {
            // Local 模式：返回后端代理接口
            return "/api/download?path=" + URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    @Override
    public UploadInfo  getUploadInfo(String path, String fileName) {
        // Local 模式：告诉前端走后端代理
        return UploadInfo.builder()
                .uploadType("PROXY")
                .postUrl("/upload") // 指向 FileController 的 upload 接口
                .build();
    }

    @Override
    public boolean uploadProxy(String path, MultipartFile file) {
        File parentDir = new File(this.rootPath, path);
        if (!parentDir.exists()) parentDir.mkdirs();

        File targetFile = new File(parentDir, file.getOriginalFilename());
        try {
            file.transferTo(targetFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public ResponseEntity<Resource> getFileResource(String path) {
        File file = new File(this.rootPath, path);
        if (!file.exists()) return ResponseEntity.notFound().build();

        try {
            String encodedFilename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .contentLength(file.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(file));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("文件名编码失败", e);
        }
    }

    @Override
    public boolean createFolder(String path, String name) {
        File newDir = new File(this.rootPath, path + "/" + name);
        return !newDir.exists() && newDir.mkdirs();
    }

    @Override
    public boolean delete(String path) {
        File file = new File(this.rootPath, path);
        return deleteRecursive(file);
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) for (File f : files) deleteRecursive(f);
        }
        return file.delete();
    }

    @Override
    public String getPassword(String path) {
        File pwdFile = new File(this.rootPath, path + "/password.txt");
        if (pwdFile.exists() && pwdFile.isFile()) {
            try {
                return new String(java.nio.file.Files.readAllBytes(pwdFile.toPath())).trim();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean rename(String path, String name, String newName) {
        File src = new File(this.rootPath, path + "/" + name);
        File dest = new File(this.rootPath, path + "/" + newName);
        return src.exists() && src.renameTo(dest);
    }
}