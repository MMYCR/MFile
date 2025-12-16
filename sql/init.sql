-- 创建数据库
CREATE DATABASE IF NOT EXISTS `mfile` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `mfile`;

-- 1. 用户表 (mfile_user)
DROP TABLE IF EXISTS `mfile_user`;
CREATE TABLE `mfile_user` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `username` varchar(50) NOT NULL COMMENT '用户名',
                              `password` varchar(100) NOT NULL COMMENT '加密密码',
                              `enable` tinyint(1) DEFAULT 1,
                              `createtime` datetime DEFAULT NULL,
                              `updatetime` datetime DEFAULT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 存储源表 (storage_source)
DROP TABLE IF EXISTS `storage_source`;
CREATE TABLE `storage_source` (
                                  `id` bigint(20) NOT NULL AUTO_INCREMENT,
                                  `enable` tinyint(1) DEFAULT 1,
                                  `name` varchar(255) DEFAULT NULL,
                                  `key` varchar(255) DEFAULT NULL,
                                  `type` varchar(50) DEFAULT 'local',
                                  `order_num` int(11) DEFAULT 0,
                                  `root_path` varchar(1024) DEFAULT NULL,
                                  `config_data` TEXT DEFAULT NULL,
                                  `user_id` bigint(20) DEFAULT NULL,
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 分享链接表 (share_link)
DROP TABLE IF EXISTS `share_link`;
CREATE TABLE `share_link` (
                              `id` bigint(20) NOT NULL AUTO_INCREMENT,
                              `uuid` varchar(50) NOT NULL COMMENT '短链接码',
                              `storage_key` varchar(50) DEFAULT NULL COMMENT '存储源Key',
                              `path` varchar(1024) DEFAULT NULL COMMENT '文件路径',
                              `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
                              `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 初始化管理员 (密码: 123456)
-- 显式指定 ID=1，确保如果有默认存储源能关联上
INSERT INTO `mfile_user` (`id`, `username`, `password`, `enable`, `createtime`, `updatetime`)
VALUES (1, 'admin', '$2a$10$eF8p/AHIF0H17YqMILPeGO3CsNQuGjchmd1.v.VHNraP7pCuwvErG', 1, NOW(), NOW());