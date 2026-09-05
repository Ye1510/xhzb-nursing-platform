# 部署说明

[← 返回 README](../README.md)

## 环境要求

| 依赖 | 版本 | 用途 |
| --- | --- | --- |
| JDK | 17 | 后端运行 |
| Maven | 3.6+ | 构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.0 | 主库 |
| Redis | 6/7 | 会话、业务缓存、IoT 产品缓存、AI 会话记忆 |
| Redis Stack | latest | 向量库（RAG），**与业务 Redis 独立部署**，默认 6378 端口 |
| Docker | 20+ | 部署（可选） |
| Nginx | 1.20+ | 静态资源与反向代理 |

> 业务 Redis 与向量库 Redis **不要用同一个实例**：向量库需要 `RediSearch` 模块且内存占用特性不同，混用会让运维口径变复杂。

---

## 1. 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE xhzb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -u root -p xhzb < sql/xhzb.sql
```

`sql/xhzb.sql` 包含建表语句与种子数据（含默认账号）。重新导入前请显式确认，脚本内含 `DELETE FROM` 语句。

---

## 2. 环境变量

所有凭证通过环境变量注入，仓库内**不包含任何明文密钥**。完整清单见 [`.env.example`](../.env.example)。

### 最小可跑集

只跑管理后台（不含 AI / IoT / OSS 功能）时，配置这些即可：

| 变量 | 说明 | 示例 |
| --- | --- | --- |
| `DB_HOST` | MySQL 地址 | `localhost` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_NAME` | 库名 | `xhzb` |
| `DB_USERNAME` | 数据库用户 | `root` |
| `DB_PASSWORD` | 数据库密码 | — |
| `REDIS_HOST` | Redis 地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码（无则留空） | — |
| `JWT_SECRET` | 令牌签名密钥 | `openssl rand -base64 48` |

### 各功能模块所需的额外变量

| 功能 | 变量 |
| --- | --- |
| AI 问答 / 健康评估 | `OPENAI_API_KEY`（阿里云百炼 DashScope）、`VECTOR_STORE_HOST`、`VECTOR_STORE_PORT` |
| 文件上传 | `OSS_ACCESS_KEY_ID`、`OSS_ACCESS_KEY_SECRET`、`OSS_BUCKET_NAME`、`OSS_ENDPOINT` |
| 设备监测 | `HUAWEI_IOT_ENDPOINT`、`HUAWEI_IOT_PROJECT_ID`、`HUAWEI_CLOUD_AK`、`HUAWEI_CLOUD_SK`、`HUAWEI_IOT_ACCESS_KEY`、`HUAWEI_IOT_ACCESS_CODE` |
| 小程序端 | `WX_MINIAPP_APPID`、`WX_MINIAPP_SECRET` |

### 配置方式

```bash
# Linux / macOS
export DB_PASSWORD="your-password"

# Windows（需重开终端生效）
setx DB_PASSWORD "your-password"

# Docker 启动时注入（推荐用于容器化部署）
docker run -e DB_PASSWORD=xxx ...
```

> 生产环境建议用密钥管理服务（KMS / Vault）或 CI 的 Secret 机制注入，而不是写进服务器的 shell profile。

---

## 3. 本地启动

### 后端

```bash
# 首次：把各模块安装到本地 Maven 仓库
mvn install -DskipTests

# 启动，主类 com.xhzb.XhzbApplication（位于 xhzb-admin）
mvn spring-boot:run -pl xhzb-admin
```

默认激活 `dev` 环境（见 `xhzb-admin/src/main/resources/application.yml` 的 `spring.profiles.active`）。

- 服务端口：`8080`
- 接口文档：<http://localhost:8080/swagger-ui.html>

其他环境：

```bash
mvn spring-boot:run -pl xhzb-admin -Dspring-boot.run.profiles=test
mvn spring-boot:run -pl xhzb-admin -Dspring-boot.run.profiles=prod
```

### 向量库

```bash
docker run -d --name redis-stack -p 6378:6379 redis/redis-stack:latest
```

### 前端

```bash
cd xhzb_ui
npm install
npm run dev
```

访问 <http://localhost:9001/>，默认账号 `admin / admin123`。

生产构建：

```bash
npm run build:prod    # 产出 dist/
npm run build:stage   # staging 环境
```

---

## 4. Docker 部署

`xhzb-admin/Dockerfile` 基于 `eclipse-temurin:17-jdk-jammy`，**镜像内不含任何凭证**。构建前先打包：

```bash
mvn clean package -DskipTests

docker build -t xhzb-admin:latest -f xhzb-admin/Dockerfile xhzb-admin/target
```

启动容器，凭证通过 `-e` 注入：

```bash
docker run -d --restart=always --name xhzb-admin \
  -p 9000:9000 \
  -v /usr/local/xhzb-admin/logs:/home/xhzb/logs \
  -e DB_HOST=192.168.1.10 -e DB_PASSWORD=xxx \
  -e REDIS_HOST=192.168.1.11 -e REDIS_PASSWORD=xxx \
  -e JWT_SECRET=xxx \
  -e OPENAI_API_KEY=xxx \
  xhzb-admin:latest
```

`xhzb-admin/deploy.sh` 封装了「停旧容器 → 删容器 → 起新容器」的滚动更新：

```bash
sh xhzb-admin/deploy.sh xhzb-admin latest
```

---

## 5. Jenkins CI/CD

`Jenkinsfile` 定义五段流水线：

| 阶段 | 动作 |
| --- | --- |
| 清除工作空间 | `cleanWs()`，避免残留产物污染构建 |
| 拉取代码 | 从 `GIT_URL` 参数指定的仓库拉分支，凭证走 `credentialsId` |
| Maven 打包 | `mvn clean install -DskipTests` |
| 构建镜像 | 遍历 `services` 参数，逐个服务打镜像 |
| 部署服务 | 调用各服务的 `deploy.sh` 完成容器替换 |

**使用前需替换的参数**

- `GIT_URL` 默认值改为自己的仓库地址
- `credentialsId: 'GIT_CREDENTIALS_ID'` 改为 Jenkins 中实际配置的凭证 ID
- 流水线中的 Maven 路径 `/var/jenkins_home/maven/bin/mvn` 需按 Jenkins 环境调整

---

## 6. Nginx 配置参考

前端静态资源与后端接口同域部署，通过路径前缀区分：

```nginx
server {
    listen       80;
    server_name  your-domain.com;

    # 前端静态资源
    location / {
        root       /usr/share/nginx/html;
        index      index.html;
        try_files  $uri $uri/ /index.html;   # 支持前端 history 路由
    }

    # 后端接口反向代理
    location /prod-api/ {
        proxy_pass       http://127.0.0.1:9000/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;

        # WebSocket 支持（消息推送）
        proxy_http_version 1.1;
        proxy_set_header   Upgrade    $http_upgrade;
        proxy_set_header   Connection "upgrade";
    }
}
```

> 前端 `xhzb_ui/.env` 中的 `VITE_APP_BASE_API` 需与代理前缀一致（dev 为 `/dev-api`，生产为 `/prod-api`）。

---

相关文档：[架构设计](architecture.md) · [技术实现要点](technical-notes.md) · [功能模块](modules.md)
