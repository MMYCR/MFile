package org.example.myselffile.module.model;


import lombok.Data;

import java.util.Date;

@Data
public class FileNode {
    private String name;    // 文件名
    private String path;    // 路径
    private Long size;      // 大小
    private Date time;      // 修改时间
    private String type;    // "FILE" 或 "FOLDER"
    private String url;     // 下载链接
}