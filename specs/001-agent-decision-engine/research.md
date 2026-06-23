# 研究报告：Spring AI + Langfuse3 演示项目

**功能**: 001-springai-langfuse3-demo
**日期**: 2026-06-04
**状态**: 已完成

## 研究主题 1：多模型集成方案

**决策**: 使用 Spring AI 原生集成 DeepSeek，MiMo 和通义千问通过
OpenAI 兼容模式接入。

**理由**:
- Spring AI 1.x 原生支持 DeepSeek（`spring-ai-deepseek` starter），
  配置简洁，开箱即用。
- 字节 MiMo 提供 OpenAI 兼容 API，可通过
  `spring-ai-openai` starter 配置自定义 `base-url` 接入。
- 通义千问（DashScope）通过 Spring AI Alibaba 的
  `spring-ai-alibaba-starter` 原生接入，同时支持 Graph 工作流。
- 三者均通过 Spring AI 的 `ChatModel` 统一接口抽象，服务层代码
  无需感知具体模型实现。

**备选方案**:
- 全部通过 OpenAI 兼容模式接入：可行但丢失 DeepSeek 和 DashScope
  的原生特性支持（如 DashScope 的 Graph 能力）。
- 使用 Ollama 本地模型：增加部署复杂度，与演示"云端 API 快速接入"
  的目标不符。

---

## 研究主题 2：流式输出三种方案

**决策**: 分别实现 Flux 响应式流式、SSE 标准流式、SseEmitter 流式
三种独立端点。

**理由**:
- **Flux 流式**: Spring AI 的 `ChatClient.prompt().stream()` 原生返回
  `Flux<ChatResponse>`，最简洁的实现方式。
- **SSE 标准流式**: 基于 Spring MVC 的 `@GetMapping(produces =
  MediaType.TEXT_EVENT_STREAM_VALUE)` 直接返回 `Flux`，利用
  框架自动 SSE 序列化。
- **SseEmitter 流式**: 使用 Spring MVC 的 `SseEmitter` 手动发送事件，
  适用于需要更精细控制的场景（如企业级中间件集成）。

**备选方案**:
- WebSocket：双向通信对 AI 对话场景过度设计。
- Server-Sent Events + JavaScript EventSource：客户端方案，
  服务端仍需上述三种之一。

---

## 研究主题 3：RAG 实现方案

**决策**: 使用 `SimpleVectorStore`（内存向量存储）+
`TokenTextSplitter` 文档分割 + `QuestionAnswerAdvisor` 检索增强。

**理由**:
- `SimpleVectorStore` 是 Spring AI 内置的内存向量存储，无需外部依赖，
  适合演示场景。
- `TokenTextSplitter` 负责将文档按 Token 分割为适合嵌入的片段。
- `QuestionAnswerAdvisor` 自动完成"检索→拼接上下文→生成回答"的
  完整 RAG 流程，支持流式输出。
- 文档摄入使用 `TokenTextSplitter` + `VectorStore.add()` 流程。
- 嵌入模型使用 DashScope 的文本嵌入模型（通过 Spring AI Alibaba）。

**备选方案**:
- `RetrievalAugmentationAdvisor`：更高级的 RAG 实现，支持
  相似度阈值过滤，可在第二阶段升级。
- 外部向量数据库（如 Chroma、PgVector）：增加部署复杂度，
  演示项目无需。

---

## 研究主题 4：Function Calling 方案

**决策**: 使用 Spring AI 的 `Function` Bean 注册 +
`ChatClient.toolNames()` 调用。

**理由**:
- Spring AI 支持通过 `@Bean` + `@Description` 注册工具函数，
  模型自动识别并调用。
- 通过 `ChatClient.prompt().toolNames("functionName").call()`
  指定可用工具。
- 演示两个工具：天气查询（模拟）和数学计算器。
- 工具实现为独立的 `@Component`，便于测试和替换。

**备选方案**:
- `FunctionToolCallback` 显式注册：更精确的控制，可在后续升级。
- `@Tool` 注解方式：更简洁但灵活性稍低。
- 使用外部 API：增加网络依赖，不适合演示。

---

## 研究主题 8：Skills 框架集成

**决策**: 使用 Spring AI Alibaba 的 Skills 框架，通过 ClasspathSkillRegistry 自动发现技能。

**理由**:
- Spring AI Alibaba 1.1.2+ 原生支持 Skills 框架
- ClasspathSkillRegistry 自动扫描 `skills/` 目录下的 SKILL.md 文件
- SkillsAgentHook 支持渐进式披露，系统提示词仅加载技能摘要
- ReactAgent 集成，支持多轮对话和工具调用

**备选方案**:
- 手动注册技能：灵活性高但维护成本大
- 使用外部配置文件：增加配置复杂度

---

## 研究主题 9：投资工具设计

**决策**: 使用 `@Tool` 注解定义投资工具，通过 `MethodToolCallbackProvider` 自动注册。

**理由**:
- Spring AI 的 `@Tool` 注解是标准的工具定义方式
- `MethodToolCallbackProvider` 自动扫描所有 `@Tool` 方法并注册
- 工具实现为独立的 `@Component`，便于测试和替换
- 投资工具提供模拟数据，支持离线测试和演示

**备选方案**:
- 使用 `Function<Request, Response>` Bean：更灵活但代码量更大
- 使用外部 API：增加网络依赖，不适合演示

---

## 研究主题 10：系统提示词管理

**决策**: 将系统提示词作为常量定义在各 Service 类中，通过 `.system()` 方法注入。

**理由**:
- 简单直接，易于理解和维护
- 每个服务的提示词独立管理，职责清晰
- 通过 Git 版本控制追踪变更
- 与 Spring AI 的 ChatClient API 无缝集成

**备选方案**:
- 外部配置文件：增加配置复杂度
- Langfuse Prompt 管理：增加外部依赖
- 数据库存储：过度设计

---

## 研究主题 11：主流程测试设计

**决策**: 创建 `ApplicationIntegrationTest` 类，串联所有核心功能进行端到端测试。

**理由**:
- 验证系统整体工作流程，而非单个功能
- 使用 `@SpringBootTest` 启动完整应用，模拟真实环境
- 使用 `TestRestTemplate` 进行 HTTP 调用，测试 API 端点
- 使用 `Assumptions` 在外部 API 不可用时优雅跳过

**备选方案**:
- 单元测试：无法验证端到端流程
- 手动测试：效率低，不可重复
- UI 自动化测试：过度设计，本项目无前端

---

## 研究主题 5：Spring AI Alibaba Graph 工作流

**决策**: 使用 `StateGraph` 定义工作流，包含分类节点、处理节点、
输出节点和条件路由边。

**理由**:
- `StateGraph` 是 Spring AI Alibaba Graph 的核心类，用于定义
  多步骤 AI 工作流。
- 通过 `addNode()` 添加处理节点，`addConditionalEdge()` 添加
  条件路由。
- `OverAllState` 管理跨节点共享状态。
- `CompiledGraph` 编译后可执行。
- 演示场景：文本分类→根据分类选择不同处理策略→汇总输出。

**备选方案**:
- 顺序链（Sequential Chain）：无条件分支，不满足演示需求。
- 自定义状态机：重复造轮子，Graph 已提供完整抽象。

---

## 研究主题 6：Langfuse3 + OpenTelemetry 集成

**决策**: 通过 OpenTelemetry OTLP 导出器将追踪数据发送至 Langfuse，
支持云端和本地 Docker 两种模式。

**理由**:
- Langfuse 3 支持 OpenTelemetry 标准协议，提供 `/api/public/otel`
  端点接收追踪数据。
- 云端模式：配置 `OTEL_EXPORTER_OTLP_ENDPOINT` 为
  `https://cloud.langfuse.com/api/public/otel`。
- 本地模式：Docker Compose 部署 Langfuse，配置端点为本地地址。
- 认证通过 `OTEL_EXPORTER_OTLP_HEADERS` 传递 Base64 编码的
  `publicKey:secretKey`。
- Spring Boot 通过 `micrometer-tracing-bridge-otel` +
  `opentelemetry-exporter-otlp` 自动集成。
- Langfuse 服务不可达时，OTel 导出器静默失败，不影响 AI 业务。

**备选方案**:
- Langfuse Java SDK 直接集成：需要手动管理追踪上下文，
  不如 OTel 自动化。
- Jaeger/Zipkin 作为中间收集器：增加架构复杂度，
  Langfuse 已原生支持 OTLP。

---

## 研究主题 7：项目模块化架构

**决策**: 按功能域划分为独立模块，每个模块可由独立子 Agent 并行开发。

**模块划分**:
1. **common** — 公共配置、模型路由、异常处理
2. **chat-streaming** — 三种流式输出端点
3. **rag** — RAG 知识库问答
4. **function-calling** — Function Calling 工具调用
5. **graph-workflow** — Graph 多步骤工作流
6. **observability** — Langfuse + OTel 可观测性配置

**理由**: 按功能域拆分使每个模块职责单一，支持并行开发。
common 模块为所有功能模块提供基础设施，其他模块互不依赖。
