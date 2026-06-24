# 基础镜像
FROM eclipse-temurin:17-jdk-jammy
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 环境变量
# 敏感凭证不写入镜像层，统一在容器启动时注入（参见 docker run 的 -e 参数与 README 的环境变量表）：
#   OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET / OPENAI_API_KEY
#   HUAWEI_CLOUD_AK / HUAWEI_CLOUD_SK / HUAWEI_IOT_ACCESS_KEY / HUAWEI_IOT_ACCESS_CODE
#   WX_MINIAPP_APPID / WX_MINIAPP_SECRET / JWT_SECRET / DB_PASSWORD / REDIS_PASSWORD


# 拷贝jar包
COPY xhzb-admin.jar /app.jar

# 暴露端口
EXPOSE 9000

# 入口
ENTRYPOINT ["java", "-jar", "/app.jar"]