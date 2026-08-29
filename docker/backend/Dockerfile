# ====== 阶段1: Maven 构建 ======
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 0) 先安装公共模块 algo-common-api（houduan 依赖 know-api，必须先 install 进本地 Maven 仓库）
#    context 根 = 仓库根（AlgoVize/），COPY 路径不再带 AlgoVize/ 前缀
COPY algo-common-api/ ./algo-common-api/
RUN mvn -B -q -f algo-common-api/pom.xml install -DskipTests

# 1) 先拷 pom，利用层缓存加速依赖下载
COPY houduan/pom.xml ./
RUN mvn dependency:go-offline -B

# 2) 拷源码
COPY houduan/src ./src

# 容器化适配：
# 1) 删除 jar 内 application.yml（含明文密码/微信密钥，改由外部挂载注入）
# 2) 把 logback 写死的 Windows 路径 D:/rizi 替换为 Linux 容器路径 /app/logs
RUN rm -f src/main/resources/application.yml \
 && sed -i 's|D:/rizi|/app/logs|g' src/main/resources/logback-spring.xml

RUN mvn clean package -DskipTests -B

# ====== 阶段2: JRE 运行 ======
FROM eclipse-temurin:17-jre
WORKDIR /app

# 拷贝构建产物
COPY --from=builder /build/target/backend-1.0.0.jar app.jar

# 上传目录（由 compose 挂载命名卷持久化）
RUN mkdir -p /app/uploads /app/logs

# 时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 加载外部配置：/app/config/application-docker.yml（由 compose 挂载）
# 激活 docker profile 使 application-docker.yml 生效
EXPOSE 80
ENTRYPOINT ["java", \
  "-jar", "/app/app.jar", \
  "--spring.config.additional-location=file:/app/config/", \
  "--spring.profiles.active=docker"]
