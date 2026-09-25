# 技术实现要点

[← 返回 README](../README.md)

本文按「遇到什么问题 → 怎么解决」组织，只记录有取舍的地方。

---

## 1. 权限模型：RBAC + 数据范围

### 问题

`@PreAuthorize` 只能回答"这个角色能不能调这个接口"，回答不了"这个角色能看到哪些数据"。养老机构如果按院区/楼层划分管理职责，只做接口级鉴权会出现跨院区越权读取。

### 做法

两层组合：

| 层级 | 手段 | 作用 |
| --- | --- | --- |
| 接口级 | Spring Security `@PreAuthorize("@ss.hasPermi('nursing:elder:list')")` | 挡掉无权限的 API 调用 |
| 按钮级 | 权限标识下发到前端，控制按钮显隐 | 减少无效请求 |
| 数据级 | 数据权限 AOP 切面 + `@DataScope` 注解 | 在 SQL 层拼入机构/部门范围条件 |

**数据范围档位**（沿用若依模型）：全部数据 / 自定义 / 本部门 / 本部门及以下 / 仅本人。

### 为什么放在 AOP 而不是 Service 里手写

数据范围条件要拼进 **Mapper 的查询参数**里，如果每个查询自己写过滤条件，一是容易漏，二是规则变化时要改几十处。用 AOP 统一在方法进入前把范围参数注入，Mapper XML 里统一引用，改规则只改切面。

**相关类**：`xhzb-framework` → `DataScopeAspect`、`SysUserServiceImpl`（角色数据范围解析）

---

## 2. 无状态鉴权：JWT + Redis

### 问题

纯 JWT 无法主动失效（用户被禁用、改密码、强制下线都拦不住），纯 Session 又不利于多端。

### 做法

JWT 存 Redis，取两者之长：

| 需求 | 实现 |
| --- | --- |
| 无状态校验 | 请求头携带 JWT，过滤器验签解析 |
| 主动失效 | Redis 中删除该用户会话，后续请求即失效 |
| 多端并存 | 会话 key 按终端区分，互不踢下线 |
| 过期控制 | 令牌 30 分钟过期（`token.expireTime`） |
| 登录风控 | 密码错误 `maxRetryCount` 次锁定 `lockTime` 分钟 |

签名密钥通过 `JWT_SECRET` 环境变量注入，**不落配置文件**。

**相关类**：`xhzb-framework` → `SecurityConfig`、`JwtAuthenticationTokenFilter`、`TokenService`

---

## 3. 缓存策略

| 场景 | 做法 | 理由 |
| --- | --- | --- |
| 字典、参数配置 | Redis 缓存 + 增删改时主动刷新 | 读多写极少，且改动即时可见 |
| IoT 产品列表 | 缓存于 `iot:all_product_list`，同步操作后刷新 | 华为云接口调用有配额与延迟，不宜每次查 |
| 楼层 / 房间 / 护理等级 | Spring Cache + Redis | 基础数据，单次页面加载多次引用 |
| 登录会话 / 令牌 | Redis（Hash 结构） | 需要按 key 精确失效 |
| 会话记忆（AI 多轮） | Redis 持久化 | 支持跨请求的多轮上下文 |

**注意点**：缓存 key 统一走 `CacheConstants` 常量类，避免散落的字符串字面量在改动时对不上。

---

## 4. RAG 检索增强问答

> 本节只保留索引。完整设计说明（两条链路对比、流程图、四个参数的取舍依据、提示词边界条件、两阶段评估与可降级设计）已独立到 **[AI 能力](ai-features.md)**。

**一句话**：机构护理规范文档切分后向量化存入 Redis Stack，提问时检索相似度 ≥ 0.7 的 top 5 片段拼进 Prompt，交给通义千问流式作答；多轮上下文按会话 ID 隔离并落 Redis 持久化。

**相关类**：`xhzb-nursing-platform` → `SpringAiConfig`、`RedisVectorConfig`、`ChatController`、`RedisChatMemoryService`
**配置**：`spring.ai.openai.*`（模型 `qwen3.7-max`、嵌入 `text-embedding-v3` 1024 维），API Key 走 `OPENAI_API_KEY` 环境变量

---

## 5. IoT 数据链路与报警引擎

### 数据链路

```
设备属性上报 → 华为云 IoTDA → AMQP 消息 → AmqpClient 订阅 → 落库 device_data → 规则匹配 → alert_data
```

**为什么用 AMQP 而不是轮询**：轮询的开销随设备数线性增长，且报警有延迟；AMQP 由平台主动推送，时效性好且开销恒定。`AmqpClient` 实现 `ApplicationRunner`，应用启动即建立订阅。

### 报警规则的五元组

规则表 `alert_rule` 存的不是单一阈值，而是：

| 字段 | 含义 | 解决什么 |
| --- | --- | --- |
| `function_id` | 设备功能标识 | 同一个设备的不同指标（如心率 vs 体温）分别定规则 |
| 运算符 + 阈值 | 如 `>` 120 | 基本判定 |
| **持续周期** | 如持续 5 分钟 | **抗抖动** —— 瞬时超标不报，避免误报疲劳 |
| **聚合周期** | 如取 10 分钟均值 | **统一统计口径** —— 按均值还是峰值判定 |

命中后写入 `alert_data`，字段包含报警原因（按规则格式化）、处理状态、处理人、处理时间，支持"待处理 → 已处理"闭环。

### 设备的唯一性约束

一个物理位置 + 一种产品只应绑定一个设备。约束建在数据库层：

```sql
UNIQUE (binding_location, location_type, physical_location_type, product_key)
```

放在数据库而不是只靠应用层校验，是因为并发注册时应用层校验存在竞态窗口。

**相关类**：`xhzb-framework` → `IotClientConfig`、`HuaWeiIotConfigProperties`；`xhzb-nursing-platform` → `AmqpClient`、`DeviceServiceImpl`

---

## 6. 其他

### 对象存储

文件走阿里云 OSS 而非本地磁盘：单机磁盘在扩容和多实例部署时会成为不一致源。`xhzb-oss` 模块封装上传/下载，`oss.bucketName`、`oss.endpoint` 均可通过环境变量切换环境。

### 消息推送

WebSocket 用于站内提醒（如新报警工单）。前端 `VITE_APP_SOCKET_URL` 配置连接地址，与 HTTP 接口分开连接。

### 定时任务

Quartz 管理护理任务类定时作业，任务本身与 Quartz 的调度记录解耦，避免调度数据与业务数据混在一张表。

### 代码生成器

`xhzb-generator` 基于 Velocity 模板，从表结构一键生成 Entity / Mapper / XML / Service / Controller / Vue 页面与菜单 SQL。用它的原因是项目里 CRUD 型模块占比高，手写重复度高且容易在命名和分页上不一致；生成后再按业务补规则。

### 前端工程化

| 关注点 | 做法 |
| --- | --- |
| 构建 | Vite 5，按 `--mode` 区分 staging / prod 环境 |
| 路由权限 | 登录后拉取菜单，动态生成路由，前端 `permission.js` 守卫 |
| 请求封装 | `utils/request.js` 统一注入 token、统一错误码处理、统一 loading |
| 按钮权限 | `v-hasPermi` 指令，与后端权限标识一致 |
| 接口分层 | `src/api/` 按业务域拆分（nursing / system / monitor / tool） |

---

相关文档：[架构设计](architecture.md) · [功能模块](modules.md) · [部署说明](deployment.md)
