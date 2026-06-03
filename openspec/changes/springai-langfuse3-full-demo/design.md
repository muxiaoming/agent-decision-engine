## Context

团队需要搭建一个 Spring AI 演示项目，覆盖聊天、RAG、Function Calling、Graph Workflow 四大场景，集成 Langfuse 3 实现全链路 OpenTelemetry 追踪。当前项目为空白目录，需要从零搭建完整的 Spring Boot 3 + Java 21 + Maven 项目。

技术栈选型已确定：
- **框架**: Spring Boot 3.x + Spring AI 1.x
- **语言**: Java 21
- **构建**: Maven
- **模型**: DeepSeek、MiMo（OpenAI 兼容协议）、DashScope（百炼通义千问）
- **可观测性**: Langfuse 3（OTLP HTTP 端点）
- **编排**: Spring AI Alibaba Graph

## Goals / Non-Goals

**Goals:**
- 搭建可运行的 Spring Boot 项目，包含完整的 Maven 依赖管理
- 实现 DeepSeek / MiMo / DashScope 三模型动态切换（通过 `ChatModel` Bean 名称路由）
- 集成 Langfuse 3 OTLP 追踪，所有 AI 调用自动生成 Trace / Span / Generation
- 实现 RAG 模块：文档加载 → 文本分割 → 向量化 → 检索 → 增强生成
- 实现 Function Calling：注册自定义工具函数，演示模型工具调用闭环
- 实现 Graph Workflow：使用 Spring AI Alibaba Graph 编排顺序/并行/条件分支工作流
- 提供 REST API 端点供前端或 curl 测试

**Non-Goals:**
- 不实现生产级认证鉴权（仅演示用途）
- 不实现复杂的向量数据库集群部署（使用 SimpleVectorStore 或 PgVector 单机）
- 不实现完整的前端 UI（仅提供 REST API + 可选的简单 HTML 页面）
- 不实现模型微调或训练流水线

## Decisions

### D1: 多模型接入策略 — 使用 OpenAI 兼容协议统一接入

**选择**: DeepSeek 和 MiMo 通过 OpenAI 兼容协议接入（`spring-ai-starter-model-openai`），DashScope 使用 Spring AI Alibaba 原生 starter。

**理由**: DeepSeek 和 MiMo 均提供 OpenAI 兼容 API，复用 OpenAI starter 可减少依赖；DashScope 需要阿里云特有鉴权机制，使用原生 starter 更稳定。

**替代方案**: 全部使用 OpenAI 兼容协议 — 但 DashScope 的部分高级功能（如流式响应优化）需要原生 SDK 支持。

### D2: 模型动态切换 — 基于 Bean 名称路由

**选择**: 为每个模型创建独立的 `ChatClient` Bean，通过 `@Qualifier` 注入或运行时 Map 路由实现动态切换。

```java
@Bean("deepseekChatClient")
ChatClient deepseekChatClient(ChatModel deepseekChatModel) { ... }

@Bean("dashscopeChatClient")
ChatClient dashscopeChatClient(ChatModel dashscopeChatModel) { ... }
```

**理由**: Spring AI 的 `ChatModel` 抽象天然支持多 Bean 注册，`@Qualifier` 是最简洁的切换方式。

### D3: Langfuse 3 集成 — OTLP HTTP Exporter 直连

**选择**: 通过环境变量配置 OTLP Exporter，将 OpenTelemetry 数据直连 Langfuse OTLP 端点，无需额外 SDK。

```yaml
OTEL_EXPORTER_OTLP_ENDPOINT: https://cloud.langfuse.com/api/public/otel
OTEL_EXPORTER_OTLP_HEADERS: Authorization=Basic <base64(pk:sk)>,x-langfuse-ingestion-version=4
```

**理由**: Langfuse 3 原生支持 OTLP 协议，Spring AI 内置 Micrometer Tracing + OTLP Exporter，零代码集成。

**替代方案**: 使用 Langfuse Java SDK 手动埋点 — 侵入性高且无法自动捕获 Spring AI 内部 Span。

### D4: RAG 向量存储 — SimpleVectorStore（开发） / PgVector（可选生产）

**选择**: 默认使用 `SimpleVectorStore`（内存存储），可选切换 `PgVector`。

**理由**: 演示项目优先简单可运行，SimpleVectorStore 无需外部依赖；提供 PgVector 配置示例供生产参考。

### D5: Graph Workflow — Spring AI Alibaba Graph Core

**选择**: 使用 `spring-ai-alibaba-graph-core` 模块，通过 `StateGraph` API 编排工作流。

**理由**: 这是 Spring AI Alibaba 官方推荐的图编排方案，支持条件路由、并行执行、状态管理，且与 Spring AI 生态无缝集成。

### D6: 项目结构 — 单模块 Maven 项目

**选择**: 单模块 Maven 项目，按功能分包（`chat`、`rag`、`function`、`graph`、`config`）。

**理由**: 演示项目体量适中，单模块足够清晰；避免多模块带来的构建复杂度。

## Risks / Trade-offs

- **[模型 API 稳定性]** → DeepSeek / MiMo API 可能变更或限流。缓解：使用标准 OpenAI 兼容协议，切换模型只需改配置。
- **[Langfuse Cloud 网络延迟]** → OTLP Exporter 直连云端可能有延迟。缓解：支持本地 Langfuse 实例部署。
- **[Spring AI Alibaba Graph 版本兼容]** → Graph 模块与 Spring AI 主版本可能存在兼容性问题。缓解：使用 BOM 统一版本管理，pom.xml 中显式声明 `spring-ai-alibaba-bom`。
- **[SimpleVectorStore 不持久化]** → 内存向量存储重启丢失。缓解：提供 PgVector 配置切换选项。
