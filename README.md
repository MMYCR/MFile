
<div align="center">

# ☁️ MySelfFile | 私有云盘系统

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.0-red?style=flat-square&logo=redis)](https://redis.io/)
[![Vue 3](https://img.shields.io/badge/Vue.js-3.4-4FC08D?style=flat-square&logo=vue.js)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.6-blue?style=flat-square&logo=element)](https://element-plus.org/)
[![License](https://img.shields.io/badge/license-MIT-yellow?style=flat-square)](./LICENSE)

**一个基于 Java 21 + Vue 3 的现代化分布式文件管理系统。**
<br>
采用前后端分离架构，集成 **Redis 高性能缓存**，支持 **本地/OSS** 多存储策略切换与 **服务端签名直传**，彻底解决大文件上传带宽瓶颈与高并发访问下的性能问题。

[在线演示](#) · [报告Bug](https://github.com/MMYCR/MySelfFile/issues) · [功能请求](https://github.com/MMYCR/MySelfFile/issues)

</div>

---

## 📖 项目简介

**MySelfFile** 旨在解决传统网盘在 **大文件传输效率**、**高并发目录查询** 及 **多存储源统一管理** 上的痛点。

项目不仅仅是一个简单的 CRUD 系统，而是针对生产环境设计了完整的 **高并发缓存方案**（防穿透/击穿/雪崩）和 **对象存储优化方案**（直传、模拟重命名）。同时集成 **Sa-Token** 实现分布式会话鉴权，并提供灵活的 **目录加密** 与 **链接分享** 功能。

## 🏗️ 系统架构

```mermaid
graph TD
    User[用户浏览器] -->|1. HTTPS| Nginx[Nginx 网关]
    Nginx -->|静态资源| Vue[Vue 3 前端]
    Nginx -->|API 请求| Boot[Spring Boot 后端]
    
    subgraph "高性能缓存层"
    Boot <-->|Cache Aside| Redis[(Redis 缓存集群)]
    end

    subgraph "存储策略层 (Strategy Pattern)"
    Boot -->|策略分发| Local[本地存储策略]
    Boot -->|策略分发| OSS[阿里云 OSS 策略]
    end
    
    Local -->|IO流写入| Disk[(服务器磁盘)]
    
    User -.->|2. 直传 (带签名)| Aliyun[(阿里云 OSS)]
    OSS -.->|1. 生成 Policy 签名| User
```

---

## 🚀 核心亮点 (Interview Highlights)

### 1. 高并发缓存架构设计
*   **多级缓存策略**：
    *   **配置缓存**：将存储源配置（AK/SK）缓存至 Redis（TTL 30min），减少数据库高频读取。
    *   **列表缓存**：采用 **Cache-Aside 模式** 缓存文件目录树，写操作（上传/删除/重命名）成功后自动清除缓存，保证数据最终一致性。
*   **缓存防护体系**：
    *   **防穿透**：针对恶意查询不存在的路径，采用 **空值缓存 (Null Object)** 策略拦截。
    *   **防雪崩**：在 Redis 过期时间上引入 **随机抖动 (Random Jitter)** 机制，防止缓存大规模并发失效。

### 2. 零带宽占用的 OSS 直传
*   **痛点**：传统模式下，上传 1GB 文件需占用应用服务器 1GB 带宽和内存，容易导致 OOM。
*   **方案**：实现了 **后端签名 -> 前端直传** 模式。后端计算 Policy 签名，前端直接 POST 数据到阿里云 OSS，将应用服务器上行带宽占用降为 **0**。

### 3. 存储策略模式与深度适配
*   **架构解耦**：定义顶层接口 `StorageStrategy`，利用 Spring IOC 容器根据 `storageKey` 动态切换 Local 或 OSS 实现类。
*   **OSS 操作优化**：
    *   **模拟重命名**：针对 S3 协议不支持 Rename 的特性，封装 **Copy Object + Delete Object** 组合操作。
    *   **路径清洗**：设计了路径归一化算法，自动处理 `/` 前缀与后缀，确保 OSS Key 符合标准规范。

### 4. 极客式隐私与分享体系
*   **非侵入式目录加密**：
    *   采用 **Hook 机制** 监听目录读取请求。若目录下存在 `password.txt` 文件，系统自动拦截请求并强制要求密码验证。
    *   **技术优势**：无需修改数据库结构，用户只需上传一个文件即可通过文件系统原生能力控制权限，实现了**权限配置与数据的解耦**。
*   **短链接分享服务**：
    *   基于 **UUID 短码** 生成带有效期的公开下载链接，支持免登录访问。
    *   结合数据库有效期校验（未来计划升级为 Redis Key 过期事件）自动清理失效链接，保障资源安全性。

---

## 🛠️ 技术栈

| 模块 | 技术选型 | 说明 |
| :--- | :--- | :--- |
| **后端** | **Java 21** | 使用最新 LTS 版本，代码更简洁 |
| | **Spring Boot 3.2** | 核心框架，移除冗余依赖 |
| | **Redis 7.0** | 分布式缓存与会话存储 |
| | **MyBatis-Plus** | 数据库 ORM，简化 CRUD |
| | **Sa-Token** | 轻量级分布式权限认证 |
| | **AWS SDK v2** | 兼容 S3 协议，对接阿里云 OSS |
| **前端** | **Vue 3.4** | Composition API + Setup 语法糖 |
| | **TypeScript** | 强类型约束 |
| | **Element Plus** | 现代化 UI 组件库 |
| **运维** | **Docker Compose** | 容器化编排 (MySQL + Redis + App) |

---

## 📂 目录结构

```text
MySelfFile/
├── mfile-web/             # Vue 3 前端源码
│   ├── src/
│   │   ├── api/           # Axios 请求封装
│   │   ├── components/    # 公共组件 (UploadButton, Preview)
│   │   └── views/         # 页面 (Login, Home, Admin, Share)
├── src/                   # Java 后端源码
│   ├── main/java/org/example/myselffile/
│   │   ├── controller/    # 控制层
│   │   ├── core/          # 全局异常、Redis工具类
│   │   ├── module/        # 核心业务模块
│   │   │   ├── storage/   # 存储引擎 (策略模式/Context)
│   │   │   └── user/      # 用户模块
│   │   └── MySelfFileApplication.java
├── docker-compose.yml     # Docker 编排文件
└── sql/                   # 数据库初始化脚本
```

---

## 📦 本地开发指南

### 1. 环境准备
*   **JDK**: 21+
*   **MySQL**: 8.0+
*   **Redis**: 5.0+
*   **Node.js**: 18+

### 2. 数据库初始化
1.  创建数据库 `mfile`。
2.  执行 `sql/init.sql` 脚本，初始化表结构和管理员账号。

### 3. 后端配置
修改 `src/main/resources/application.yml`，**或者**新建 `application-dev.yml` 配置你本地的数据库密码：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mfile
    password: 你的真实密码
  data:
    redis:
      password: 你的真实密码
```

### 4. 启动项目
*   **后端**：运行 `MySelfFileApplication.java`。
*   **前端**：在 `mfile-web` 目录下运行 `npm run dev`。

---

## 🐳 Docker 生产环境部署 (推荐)

本项目支持 Docker Compose 一键部署，且配置已脱敏。

### 1. 下载源码
```bash
git clone https://gitee.com/你的用户名/MySelfFile.git
cd MySelfFile
```

### 2. 配置环境变量 (关键步骤)
在项目根目录下创建一个 `.env` 文件（此文件包含敏感信息，**请勿提交到 Git**），填入你的真实密码：

```properties
# .env 示例
MYSQL_PWD=MyStrongPassword123
REDIS_PWD=MyStrongRedisPwd123
JWT_SECRET=RandomSecretKeyForToken
MYSQL_PORT=3306
REDIS_PORT=6379
APP_PORT=80
```

### 3. 启动服务
Docker 会自动读取 `.env` 文件并注入配置。

```bash
docker-compose up -d --build
```

启动成功后，访问 `http://localhost:80` 即可使用。

---

## ⚙️ 配置说明

| 配置项 | 说明 | 默认值/备注 |
| :--- | :--- | :--- |
| **存储源配置** | **不需要写在配置文件中** | 请登录 Admin 后台，在“存储源管理”中动态添加 AK/SK，支持热加载。 |
| **文件大小限制** | `spring.servlet.multipart` | 默认 10GB，可在 yml 中修改。 |
| **Sa-Token** | `jwt-secret-key` | 生产环境请务必修改为随机字符串。 |

---

## 📖 使用指南

### 🔒 文件夹加密
想要给某个文件夹加密？
1.  新建一个文本文档，命名为 `password.txt`。
2.  在文件里写上你的密码（例如 `123456`）。
3.  将该文件上传到你需要加密的文件夹中。
4.  **完成！** 再次访问该文件夹时，系统会自动弹出密码输入框。

### 📤 文件分享
1.  在文件列表中点击“分享”按钮。
2.  系统会自动生成一个有效期为 1 天的短链接。
3.  复制链接发送给好友，对方无需登录即可下载。

---

## 👤 作者

**MMYCR**

---

## 📄 许可证

本项目采用 [MIT](https://opensource.org/licenses/MIT) 许可证。
```