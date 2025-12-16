#  基础镜像
FROM openjdk:21-jdk-slim

#  作者信息
LABEL maintainer="MMYCR"

#  工作目录
WORKDIR /app

# 挂载点
VOLUME /app/logs
VOLUME /app/data

COPY target/mfile.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动参数
ENTRYPOINT ["java", "-jar", "app.jar"]