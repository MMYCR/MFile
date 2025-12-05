# 1. 基础镜像
FROM openjdk:21-jdk-slim

# 2. 作者信息 (已更新)
LABEL maintainer="MMYCR"

# 3. 工作目录
WORKDIR /app

# 4. 挂载点 (日志和上传文件)
VOLUME /app/logs
VOLUME /app/data

# 5. 复制构建好的 Jar 包 (注意这里名字改成了 mfile.jar)
COPY target/mfile.jar app.jar

# 6. 暴露端口
EXPOSE 8080

# 7. 启动参数
# 优化 JVM 参数，适应容器环境
ENTRYPOINT ["java", "-jar", "app.jar"]