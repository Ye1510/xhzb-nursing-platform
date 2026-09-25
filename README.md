# 星海智伴 · 智慧养老护理管理平台

面向养老机构的业务管理系统，覆盖 **来访参观 → 入住办理 → 在住照护 → 服务执行 → 退住办理** 的完整业务闭环，并在其上叠加 **AI 健康评估、RAG 智能问答、IoT 设备监测与报警** 三项智能化能力。

> **解决什么问题**：养老院日常运营里，护理等级评定、服务项目派工、家属沟通、异常监测往往分散在纸质台账和多个工具中。本系统把这些流程收敛到一套后台，并把健康评估与异常报警交给大模型和物联网设备处理，减少人工巡检与主观判断。

**定位**：基于 [RuoYi-Vue v3.9.0](https://gitee.com/y_project/RuoYi-Vue) 二次开发 —— 框架骨架沿用若依，养老业务模块自研。

---

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.5.0、MyBatis-Plus 3.5.7、Spring Security + JWT |
| 前端 | Vue 3、Element Plus、Vite 5、Pinia |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis（会话 / 业务缓存 / IoT 产品缓存）+ Redis Stack（AI 向量存储） |
| AI | Spring AI 1.1.2 + 阿里云百炼 DashScope（通义千问 + text-embedding-v3） |
| 物联网 | 华为云 IoTDA（设备注册、属性上报、AMQP 数据订阅） |
| 对象存储 | 阿里云 OSS |
| 其他 | WebSocket 消息推送、Quartz 定时任务、Jenkins + Docker 部署 |

---

## AI 能力

项目里的大模型用在**两个场景**上，链路设计完全不同 —— 这是最容易被混淆、也最能体现设计取舍的地方：

| 场景 | 解决什么 | 知识来源 | 用到检索吗 |
| --- | --- | --- | --- |
| **星海智询** | 护理员日常问答 | 机构自己的护理规范文档 | ✅ 检索增强（RAG） |
| **AI 健康评估** | 入住前评定护理能力等级 | 民政部评估标准，直接写进提示词 | ❌ 与向量库无关 |

**星海智询**：护理规范文档切分后向量化存入 Redis Stack → 提问时检索出相似度 ≥ 0.7 的 top 5 片段 → 拼进 Prompt 交给通义千问 → 流式返回。多轮上下文按会话 ID 隔离并持久化到 Redis。

**AI 健康评估**：把评估标准结构化成提示词，让模型按规则输出能力等级与升级理由。拆成两阶段 —— 能力评估（必做）+ 体检报告解读（可选），后者解析失败可降级，不阻断主流程。

> 完整设计说明（两条链路的流程图、四个检索参数的取舍、提示词的边界条件、为什么必须拆两阶段）见 **[AI 能力](docs/ai-features.md)**。

---

## 核心功能

| 模块 | 内容 |
| --- | --- |
| **服务管理** | 护理项目、护理计划、护理等级的维护与关联 |
| **入退管理** | AI 辅助的六维健康评估、入住办理（床位/合同/档案/等级单事务）、来访预约 |
| **在住管理** | 老人档案、护理员分配、家属绑定、带设备实时状态的智能床位视图 |
| **星海智询** | 基于知识库的 RAG 检索增强问答，支持流式输出与会话记忆 |
| **智能监测** | 设备管理、数据上报落库、含持续周期与聚合周期的报警规则引擎 |
| **小程序端** | 微信登录、家属预约与查询 |

各模块的业务背景与流程见 [功能模块](docs/modules.md)。

---

## 快速开始

依赖：JDK 17、Maven 3.6+、Node.js 18+、MySQL 8.0、Redis（另需 Redis Stack 作为向量库）

```bash
# 1. 初始化数据库
mysql -u root -p -e "CREATE DATABASE xhzb DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p xhzb < sql/xhzb.sql

# 2. 配置环境变量（完整清单见 .env.example）
export DB_HOST=localhost DB_USERNAME=root DB_PASSWORD=your-pwd
export REDIS_HOST=localhost JWT_SECRET=$(openssl rand -base64 48)

# 3. 向量库（AI 功能需要，其余功能可跳过）
docker run -d --name redis-stack -p 6378:6379 redis/redis-stack:latest

# 4. 启动后端
mvn install -DskipTests
mvn spring-boot:run -pl xhzb-admin

# 5. 启动前端
cd xhzb_ui && npm install && npm run dev
```

- 前端：<http://localhost:9001/>
- 接口文档：<http://localhost:8080/swagger-ui.html>
- 默认账号：`admin / admin123`

详细的依赖说明、Docker / Jenkins 部署与 Nginx 配置见 [部署说明](docs/deployment.md)。

---

## 文档

| 文档 | 内容 |
| --- | --- |
| [**AI 能力**](docs/ai-features.md) | 星海智询与健康评估两条链路的设计说明、检索参数取舍、提示词边界条件、可降级设计 |
| [架构设计](docs/architecture.md) | 模块划分与依赖方向、分层约定、请求链路图、核心数据模型、关键设计取舍 |
| [功能模块](docs/modules.md) | 六大模块的业务背景、关键流程与涉及接口 |
| [技术实现要点](docs/technical-notes.md) | 权限模型、无状态鉴权、缓存策略、RAG 参数取舍、报警引擎、前端工程化 |
| [部署说明](docs/deployment.md) | 环境变量完整清单、本地启动、Docker 部署、Jenkins CI/CD、Nginx 配置 |
| [用 AI 工具开发](docs/ai-assisted-dev.md) | 开发过程如何用 AI 编程工具：项目级上下文、人机分工边界、踩过的坑 |
| [接口文档索引](docs/api/README.md) | 8 份手写字段级接口说明（AI 评估、入住、IoT 设备、小程序登录） |

---

## 项目结构

```
.
├── docs/                     # 架构 / 模块 / 部署 / AI 实践 等专题文档
│   └── api/                  # 手写字段级接口说明
├── sql/xhzb.sql              # 建表与种子数据
├── bin/                      # 启停脚本（Windows）
├── Jenkinsfile               # CI/CD 流水线定义
├── CLAUDE.md                 # 项目级 AI 上下文
├── xhzb-admin/               # 应用入口 + 多环境配置 + Dockerfile / deploy.sh
├── xhzb-common/              # 通用能力
├── xhzb-framework/           # Spring Security + JWT、数据权限 AOP、华为云 IoT 客户端
├── xhzb-system/              # 系统管理域（用户/角色/菜单/部门/字典/岗位）
├── xhzb-generator/           # 代码生成器
├── xhzb-quartz/              # 定时任务
├── xhzb-oss/                 # 阿里云 OSS 文件存储
├── xhzb-nursing-platform/    # ★ 核心业务模块（养老护理 / AI / IoT）
└── xhzb_ui/                  # Vue 3 前端
```

---

## 开发说明与分工

本项目基于 **RuoYi-Vue v3.9.0** 二次开发，边界如下：

- **沿用框架**：`xhzb-system` / `xhzb-generator` / `xhzb-quartz` / `xhzb-common` / `xhzb-framework` 的基础骨架，以及用户、角色、菜单、部门、字典、定时任务、代码生成等系统管理功能
- **自研**：`xhzb-nursing-platform` 全部业务模块（服务管理 / 入退管理 / 在住管理 / 健康评估 / 知识库问答 / IoT 设备与报警）、`xhzb_ui` 的养老业务页面，以及 AI 与 IoT 的全部集成

<!-- TODO(自己补充)：把下面这句换成你实际负责的模块，面试被追问时能逐条展开 -->
> **本人在项目中负责**：`<待填写 —— 例如：健康评估模块、星海智询 RAG 问答、IoT 设备管理与报警规则引擎>`

---

## License

[MIT](LICENSE)

若依框架部分版权归 [RuoYi](https://gitee.com/y_project/RuoYi-Vue) 所有，遵循其 MIT 协议。
