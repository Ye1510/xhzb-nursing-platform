# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Identity

星海智伴 (XingHai ZhiBan) — a smart elderly-care nursing management platform built on **RuoYi-Vue v3.9.0**. Java 17, Spring Boot 3.5.0, MyBatis-Plus 3.5.7, Vue 3 + Element Plus frontend.

## Build & Run Commands

### Backend (Maven, from project root)

```bash
# Compile all modules
mvn clean compile

# Package (skip tests)
mvn clean package -DskipTests

# Run tests
mvn test

# Run a single test class
mvn test -pl xhzb-nursing-platform -Dtest=HealthAssessmentServiceImplTest

# Start the application (main class: com.xhzb.XhzbApplication in xhzb-admin)
mvn spring-boot:run -pl xhzb-admin
```

Active Spring profile: `dev` (set in `xhzb-admin/src/main/resources/application.yml`).

### Frontend (xhzb_ui/)

```bash
cd xhzb_ui
npm run dev            # Vite dev server
npm run build:prod     # Production build
npm run build:stage    # Staging build
```

## Module Architecture

This is a multi-module Maven project (`groupId: com.xhzb`). The root `pom.xml` declares all 8 modules:

| Module | Role |
|---|---|
| `xhzb-admin` | **Application entry point** — `XhzbApplication`, Spring configs (`application-*.yml`), system controllers (auth, user, role, menu, monitor) |
| `xhzb-common` | Shared utilities — `BaseController`, `AjaxResult`, page helpers, enums, exceptions, XSS filter |
| `xhzb-framework` | Framework — Spring Security + JWT, data scope AOP, datasource routing, interceptors, **Huawei IoT client config** |
| `xhzb-system` | Core system domain — user/role/menu/dept/dict/post management (standard RuoYi) |
| `xhzb-generator` | Velocity-based code generator for CRUD scaffolding |
| `xhzb-quartz` | Quartz scheduled task management |
| `xhzb-oss` | Alibaba Cloud OSS file storage |
| `xhzb-nursing-platform` | **Core business module** — elderly nursing: beds, floors, rooms, nursing levels/plans/projects, knowledge base, AI chat, health assessment, IoT device management |
| `xhzb_ui` | Vue 3 frontend (Vite build, not a Maven module) |

## Backend Layered Architecture

Every feature follows the standard RuoYi 3-layer pattern inside its module (typically `xhzb-nursing-platform`):

```
com.xhzb.nursing
  ├── controller/     → @RestController, extends BaseController, returns AjaxResult
  ├── service/        → I*Service interfaces
  │   └── impl/       → @Service implementations, use @Autowired for dependency injection
  ├── mapper/         → MyBatis-Plus BaseMapper<T> extensions + XML in resources/mapper/nursing/
  └── domain/         → @TableName entities + dto/ and vo/ sub-packages
```

**Controller pattern**: All controllers return `AjaxResult` (from `xhzb-common`). Use `startPage()` from `BaseController` before list queries for automatic PageHelper pagination. Use `@Log` annotation for operation logging.

**Mapper pattern**: Interfaces extend `BaseMapper<T>` from MyBatis-Plus for auto-generated CRUD. Complex queries go in hand-written XML files under `resources/mapper/nursing/`.

**Exception handling**: Throw `ServiceException` (from `xhzb-common`) for business-logic errors; throw `BaseException` for infrastructure/integration failures (e.g. IoT API errors).

## Database

- **Schema**: `xhzb` on MySQL (`xhzb-admin/src/main/resources/application-dev.yml`)
- **Init script**: `sql/xhzb.sql` (single file with schema + seed data)
- **ORM**: MyBatis-Plus with `BaseMapper`, auto-increment IDs, camelCase column mapping
- **Pagination**: PageHelper (`startPage()` in controllers)

### Key Business Tables (all in `sql/xhzb.sql`)

| Table | Purpose |
|---|---|
| `elder`, `nursing_elder` | Elderly resident profiles |
| `check_in`, `check_in_config`, `contract` | Admission/check-in workflow |
| `floor`, `room`, `room_type`, `bed` | Facility layout |
| `nursing_level`, `nursing_plan`, `nursing_project`, `nursing_project_plan` | Nursing care plans |
| `nursing_task` | Scheduled nursing tasks |
| `health_assessment`, `health_assessment_data_collection`, `health_assessment_report` | AI-powered health assessments |
| `knowledge_base` | RAG knowledge base for AI chat |
| `device`, `device_data` | IoT device management and telemetry |
| `alert_data`, `alert_rule` | IoT alert/notification pipeline |
| `family_member`, `family_member_elder` | Family member associations |
| `reservation` | Bed/room reservations |

Standard RuoYi system tables (`sys_user`, `sys_role`, `sys_menu`, etc.) are also present.

## Huawei Cloud IoT Integration

The project integrates with **华为云 IoTDA** (Huawei Cloud IoT Device Access) via the SDK `huaweicloud-sdk-iotda` v3.1.76.

- **Config bean**: `IoTDAClient` created in `IotClientConfig.java` (xhzb-framework), injected as a Spring Bean.
- **Credentials**: Loaded from `HuaWeiIotConfigProperties` (`@ConfigurationProperties(prefix = "huaweicloud")`), defined in `application-dev.yml`.
- **Product list**: Synced from IoT platform and cached in Redis under key `iot:all_product_list` (`CacheConstants.IOT_ALL_PRODUCT_LIST`). Use `DeviceServiceImpl.syncProductList()` and `allProduct()`.
- **Device registration**: Calls Huawei IoT API to create a device, then persists to local `device` table. The `device` table has a unique constraint on `(binding_location, location_type, physical_location_type, product_key)` preventing duplicate location-product bindings.
- **Device data flow**: IoT devices report data → Huawei Cloud AMQP → application receives and stores in `device_data` table. Alerts matched against `alert_rule` rules generate `alert_data` records.
- **AMQP config**: Connection credentials (`accessKey`, `accessCode`, `queueName`) are in the `huaweicloud` config block.

### IoT API Documentation

API specs are in the `doc/` directory:
- `doc/IOT-注册设备API.md` — register a device
- `doc/IOT-查询设备详细数据API.md` — get device detail (merges local DB + Huawei cloud data)
- `doc/IOT-查询设备上报的数据API.md` — query device telemetry data

## Spring AI Integration

Two `ChatClient` beans are defined in `SpringAIConfig.java`:

1. **`chatClientByAssessment`** — health assessment expert (system prompt in Chinese medical domain)
2. **`openAiChatClient`** (named "XiaoZhi") — general nursing assistant with:
   - RAG via `QuestionAnswerAdvisor` backed by `RedisVectorStore` (similarity ≥ 0.7, topK 5)
   - Chat memory via `MessageChatMemoryAdvisor` (Redis-backed `RedisChatMemoryService`)
   - Token text splitting: 500-token chunks, 200-char minimum

**Model endpoint**: Aliyun DashScope compatible API (`https://dashscope.aliyuncs.com/compatible-mode`), model `qwen3.7-max`, embedding `text-embedding-v3` (1024 dims). API key from `OPENAI_API_KEY` env var.

**Redis instances**: Main Redis on `${REDIS_HOST:localhost}:6379` (session/token cache, IoT product cache, chat memory); Vector store Redis on `${VECTOR_STORE_HOST:localhost}:6378` (RAG embeddings).

## Health Assessment Feature

AI-powered elderly health evaluation with three tables: `health_assessment` (assessment record), `health_assessment_data_collection` (6 JSON sections: basic info, health assessment, daily living, mental state, perception/communication, social participation), `health_assessment_report` (AI-generated report). API docs in `doc/AI评估API.md`, prompt reference in `doc/AI评估API 两个参考的prompt.md`.

## Security

Spring Security + JWT stateless authentication (30-minute token expiry). Permissions via `@PreAuthorize` with `hasPermi` expressions. Data-scope filtering via AOP aspect in `xhzb-framework`.

Run as admin: default credentials `admin/admin123`.

## API Documentation

SpringDoc/OpenAPI is configured. API docs available at `/swagger-ui.html` when the app is running.

## AI 辅助开发约定

本项目使用 Claude Code 作为主要 AI 编程助手，`CLAUDE.md`（即本文件）作为项目级上下文，向 AI 说明模块架构、分层规范、建表约定与集成方式，避免每次对话重复交代背景。

开发新功能时遵循以下约定：
- Controller / Service / Mapper / Entity 分层参照 `xhzb-nursing-platform` 下已有的业务模块
- 统一返回 `AjaxResult`，列表查询前调用 `startPage()` 走 PageHelper 分页
- 业务异常抛 `ServiceException`，外部集成失败抛 `BaseException`
- 新增表后同步补充 `sql/xhzb.sql` 与对应 Mapper XML

> 更细的项目级约定可自行沉淀为 Claude Code 的 Skills 或 Rules 文件后放入 `.claude/` 目录。
