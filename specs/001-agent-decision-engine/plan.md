# 实施计划：Spring AI + Langfuse3 完整演示项目

**分支**: `001-springai-langfuse3-demo` | **日期**: 2026-06-04 | **规格**: [spec.md](spec.md)

**输入**: 功能规格说明 `specs/001-springai-langfuse3-demo/spec.md`

## 摘要

构建一个基于 Java 21 + Spring Boot 3 的 Spring AI 演示项目，集成
DeepSeek、MiMo、通义千问三个 AI 模型，实现 Flux/SSE/SseEmitter
三种流式输出、RAG 知识库问答、Function Calling 工具调用、
Spring AI Alibaba Graph 工作流，并通过 OpenTelemetry 将全链路追踪
数据上报至 Langfuse（支持云端和 Docker 本地两种部署模式）。

## 技术上下文

**语言/版本**: Java 21（LTS）

**主要依赖**:
- Spring Boot 3.4.x
- Spring AI 1.0.0（含 spring-ai-starter-model-deepseek、spring-ai-starter-model-openai）
- Spring AI Alibaba 1.0.0.4（DashScope 集成 + Graph 工作流）
- Micrometer Tracing + OpenTelemetry OTLP 导出器
- JUnit 5 + Mockito + Spring Boot Test

**存储**: SimpleVectorStore（内存向量存储，RAG 用）

**测试**: JUnit 5 + Mockito + Spring Test（Maven Surefire）

**目标平台**: 跨平台（JVM），Linux/macOS/Windows

**项目类型**: Web 服务（REST API）

**性能目标**: 首个流式片段 3 秒内到达，Langfuse Trace 30 秒内可见

**约束**:
- Java 版本 ≥ 21
- API 密钥通过环境变量注入，不得硬编码
- 所有文档和提交信息使用简体中文

**规模/范围**: 演示项目，6 个用户场景，约 8-12 个 REST 端点

## 宪章合规检查

*门禁：Phase 0 研究前必须通过。Phase 1 设计后重新检查。*

| 原则 | 状态 | 说明 |
|------|------|------|
| I. 多模型灵活切换 | ✅ 通过 | DeepSeek 原生 + MiMo OpenAI 兼容 + DashScope 原生 |
| II. 流式优先架构 | ✅ 通过 | 三种流式机制各有独立端点 |
| III. 可观测性驱动 | ✅ 通过 | OTel OTLP → Langfuse，云端+本地双模式 |
| IV. 测试先行 | ✅ 通过 | JUnit 5，mvn test 全量通过后提交 |
| V. AI 原生功能完备 | ✅ 通过 | RAG + Function Calling + Graph 各有独立模块 |
| VI. 双模式部署 | ✅ 通过 | application-cloud.yml + application-local.yml |
| VII. 统一简体中文 | ✅ 通过 | 文档、注释、提交信息均使用简体中文 |

## 项目结构

### 文档（本功能）

```text
specs/001-springai-langfuse3-demo/
├── plan.md              # 本文件（实施计划）
├── research.md          # Phase 0 研究报告
├── data-model.md        # Phase 1 数据模型
├── quickstart.md        # Phase 1 快速启动指南
├── contracts/
│   └── api.md           # Phase 1 API 接口契约
└── tasks.md             # Phase 2 任务清单（/speckit-tasks 生成）
```

### 源代码（仓库根目录）

```text
springai-langfuse3-demo/
├── pom.xml
├── docker/
│   └── docker-compose-langfuse.yml
├── src/main/java/com/example/ai/
│   ├── SpringAiDemoApplication.java
│   ├── common/
│   │   ├── config/
│   │   │   ├── AiModelConfig.java          # 多模型配置类
│   │   │   └── ChatClientConfig.java        # ChatClient Bean 配置
│   │   ├── model/
│   │   │   ├── ChatRequest.java             # 对话请求 DTO
│   │   │   ├── ChatResponse.java            # 对话响应 DTO
│   │   │   └── TokenUsage.java              # Token 用量 DTO
│   │   ├── router/
│   │   │   └── ModelRouter.java             # 模型路由器
│   │   └── exception/
│   │       ├── ModelNotAvailableException.java
│   │       └── GlobalExceptionHandler.java
│   ├── chat/
│   │   ├── controller/
│   │   │   ├── ChatController.java          # 同步对话端点
│   │   │   ├── FluxStreamController.java    # Flux 流式端点
│   │   │   ├── SseStreamController.java     # SSE 标准流式端点
│   │   │   └── SseEmitterController.java    # SseEmitter 流式端点
│   │   └── service/
│   │       └── ChatService.java             # 对话服务层
│   ├── rag/
│   │   ├── controller/
│   │   │   └── RagController.java           # RAG 问答端点
│   │   ├── service/
│   │   │   ├── RagService.java              # RAG 服务层
│   │   │   └── DocumentIngestionService.java # 文档摄入服务
│   │   └── config/
│   │       └── VectorStoreConfig.java       # 向量存储配置
│   ├── tools/
│   │   ├── controller/
│   │   │   └── ToolChatController.java      # Function Calling 端点
│   │   ├── service/
│   │   │   ├── ToolChatService.java         # 工具调用服务
│   │   │   ├── WeatherToolService.java      # @Tool 天气查询工具
│   │   │   └── CalculatorToolService.java   # @Tool 数学计算工具
│   │   └── config/
│   │       └── ToolCallbackConfig.java      # ToolCallback 统一注册
│   ├── graph/
│   │   ├── controller/
│   │   │   └── GraphController.java         # Graph 工作流端点
│   │   ├── service/
│   │   │   └── GraphWorkflowService.java    # Graph 工作流服务
│   │   └── config/
│   │       └── WorkflowGraphConfig.java     # Graph 定义配置
│   └── observability/
│       ├── config/
│       │   ├── OtelConfig.java              # OTel 配置
│       │   └── LangfuseConfig.java          # Langfuse 双模式配置
│       └── controller/
│           └── ObservabilityController.java  # 可观测性健康检查
├── src/main/resources/
│   ├── application.yml
│   ├── application-cloud.yml
│   ├── application-local.yml
│   └── docs/                                # RAG 示例文档
│       └── sample.txt
└── src/test/java/com/example/ai/
    ├── chat/
    │   └── ChatControllerTest.java
    ├── rag/
    │   └── RagServiceTest.java
    ├── tools/
    │   └── ToolChatServiceTest.java
    ├── graph/
    │   └── GraphWorkflowServiceTest.java
    └── common/
        └── ModelRouterTest.java
```

**结构决策**: 采用单体 Spring Boot 项目，按功能域（chat/rag/tools/graph/
observability）划分子包。每个功能域包含 controller、service、config
三层，职责清晰，支持子 Agent 按模块并行开发。

## 复杂度追踪

> **仅在宪章合规检查存在必须说明的违规时填写**

本项目无宪章违规。所有设计决策均与宪章原则对齐。

---

## 模块依赖关系（并行开发指南）

```text
common（基础设施）
  ├── chat-streaming（依赖 common）
  ├── rag（依赖 common）
  ├── function-calling（依赖 common）
  ├── graph-workflow（依赖 common，使用 Spring AI Alibaba）
  └── observability（依赖 common，被其他模块间接使用）

并行度：
  - common 必须最先完成
  - chat-streaming / rag / function-calling / graph-workflow 可并行开发
  - observability 可与功能模块并行开发（通过配置而非代码耦合）
```
