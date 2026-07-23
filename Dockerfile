# ── 阶段 1：Maven 构建 ──────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# 先拷贝 pom.xml 并拉取依赖（利用 Docker 缓存加速）
COPY pom.xml .
RUN mvn dependency:go-offline -B -q || true
# 拷贝源码并打包（跳过测试）
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# ── 阶段 2：运行时镜像 ──────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app
# 拷贝构建产物（jar 文件名与 pom.xml 的 artifactId 和 version 一致）
COPY --from=build /build/target/*.jar app.jar

EXPOSE 2867

# JVM 参数：堆内存 512M 起步、支持容器内存限制、UTF-8 编码
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
