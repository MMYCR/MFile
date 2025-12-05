
<div align="center">

# ☁️ MySelfFile | 私有云盘系统

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue.js-3.4-4FC08D?style=flat-square&logo=vue.js)](https://vuejs.org/)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.6-blue?style=flat-square&logo=element)](https://element-plus.org/)
[![License](https://img.shields.io/badge/license-MIT-yellow?style=flat-square)](./LICENSE)

**一个基于 Java 21 + Vue 3 的现代化分布式文件管理系统。**
<br>
采用前后端分离架构，支持 **本地/OSS** 多存储策略，实现了 **服务端签名直传**，彻底解决大文件上传的带宽瓶颈问题。

[在线演示](#) · [报告Bug](https://github.com/mmycr/zfile-refactor/issues) · [功能请求](https://github.com/mmycr/zfile-refactor/issues)

</div>

---

## 📖 项目简介

**MySelfFile** 是为了解决传统网盘项目在 **大文件传输** 和 **多存储源管理** 上的痛点而设计的。

不同于传统的流式代理上传（Client -> Server -> OSS），本项目针对对象存储场景设计了 **服务端签名直传（Server-Signed Direct Upload）** 方案，让前端直接与云存储交互，将应用服务器的 IO 压力降至最低。

同时，项目集成了 **Sa-Token** 权限认证与 **策略模式** 设计，代码结构清晰，易于扩展。

## 🏗️ 系统架构

```mermaid
graph TD
    User[用户浏览器] -->|1. HTTPS| Nginx[Nginx 网关]
    Nginx -->|静态资源| Vue[Vue 3 前端]
    Nginx -->|API 请求| Boot[Spring Boot 后端]
    
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

### 1. 零带宽占用的 OSS 直传
*   **痛点**：传统模式下，上传 1GB 文件需占用应用服务器 1GB 带宽和内存，容易导致 OOM。
*   **方案**：实现了 **后端签名 -> 前端直传** 模式。
   *   后端计算 `Policy` 和 `Signature`（限制文件大小、类型、过期时间）。
   *   前端拿到签名后，直接 `POST` 数据到阿里云 OSS 域名。
*   **收益**：服务器上行带宽占用降为 **0**，单机并发上传能力提升 **10倍+**。

### 2. 存储策略模式 (Strategy Pattern)
*   定义顶层接口 `StorageStrategy`，规范 `list`, `upload`, `getDownloadUrl` 行为。
*   利用 Spring IOC 容器，通过 `StorageSourceContext` 工厂，根据 `storageKey` 动态切换 **LocalStrategy** 或 **AliyunStrategy**，支持热加载配置。

### 3. 企业级安全设计
*   **鉴权**：集成 **Sa-Token**，实现精细化权限控制与 Token 续期。
*   **防越权**：基于 `CanonicalPath` 的路径校验算法，彻底修复目录遍历（Path Traversal）漏洞。
*   **隐私**：支持文件夹密码保护，关键配置（AK/SK）数据库加密存储。

---

## 🛠️ 技术栈

| 模块 | 技术选型 | 说明 |
| :--- | :--- | :--- |
| **后端** | **Java 21** | 使用最新 LTS 版本，启用虚拟线程（可选） |
| | **Spring Boot 3.2** | 核心框架，移除冗余依赖 |
| | **MyBatis-Plus** | 数据库 ORM，简化 CRUD |
| | **Sa-Token** | 轻量级 Java 权限认证框架 |
| | **AWS SDK v2** | 兼容 S3 协议，实现 OSS 对接 |
| **前端** | **Vue 3.4** | Composition API + Setup 语法糖 |
| | **TypeScript** | 强类型约束，提升代码质量 |
| | **Vite 5** | 极速构建工具 |
| | **Element Plus** | Mac OS 风格的现代化 UI 组件库 |
| **运维** | **Docker** | 容器化部署 |
| | **Nginx** | 反向代理与动静分离 |

---

## 📂 目录结构

```text
MySelfFile/
├── mfile-web/             # Vue 3 前端源码
│   ├── src/
│   │   ├── api/           # Axios 请求封装
│   │   ├── components/    # 公共组件 (如 UploadButton)
│   │   └── views/         # 页面 (Login, Home, Admin)
│   └── vite.config.ts     # Vite 配置
├── src/                   # Java 后端源码
│   ├── main/java/org/example/myselffile/
│   │   ├── controller/    # 控制层
│   │   ├── module/        # 核心业务模块
│   │   │   ├── storage/   # 存储引擎 (策略模式核心)
│   │   │   └── user/      # 用户模块
│   │   └── MySelfFileApplication.java
└── pom.xml                # Maven 依赖
```

---

## 📦 快速开始

### 1. 环境准备
*   **JDK**: 21+
*   **Node.js**: 18+ (推荐 20.x)
*   **MySQL**: 8.0+

### 2. 后端启动
1.  创建数据库 `storage_v2`。
2.  修改 `src/main/resources/application.yml` 配置数据库连接。
3.  运行：
    ```bash
    mvn clean package -DskipTests
    java -jar target/mfile-0.0.1-SNAPSHOT.jar
    ```

### 3. 前端启动
```bash
cd mfile-web
npm install
npm run dev
```
访问 `http://localhost:5173` 即可体验。

---

## 🐳 Docker 一键部署

```bash
# 1. 编译打包
npm run build
mvn clean package

# 2. 构建镜像
docker build -t mfile:v1 .

# 3. 运行容器
docker run -d -p 80:80 --name mfile \
  -v /data/files:/root/zfile/data \
  mfile:v1
```

---

## 👤 作者

**MMYCR**

*   Github: [@MMYCR](https://github.com/MMYCR)

---

## 📄 许可证

本项目采用 [MIT](https://opensource.org/licenses/MIT) 许可证。