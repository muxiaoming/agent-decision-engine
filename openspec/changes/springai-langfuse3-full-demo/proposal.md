## Why

团队需要一个完整的 Spring AI 演示项目，用于验证多模型（DeepSeek、MiMo、DashScope）统一切换能力，以及在 Langfuse 3 可观测性平台下进行全链路 OpenTelemetry 追踪。目前缺少覆盖聊天、RAG、Function Calling、Graph Workflow 四大场景的端到端示例，导致在选型和 PoC 阶段缺乏可运行的参考实现。

## What Changes

- 新建 Spring Boot 3 + Java 21 + Maven 多模块项目骨架
- 集成 Spring AI 统一 ChatModel 抽象，接入 DeepSeek、MiMo、DashScope（百炼通义千问）三个模型供应商
- 集成 Langfuse 3 作为 OTel 可观测性后端，实现 Traces / Spans / Generations 全链路追踪
- 实现基础聊天（Chat）接口，支持模型动态切换
- 实现 RAG（检索增强生成）模块，基于向量数据库 + 文档加载
- 实现 Function Calling 模块，演示工具调用链路
- 集成 Spring AI Alibaba Graph，实现图编排工作流（顺序、并行、条件分支）
- 配置 OpenTelemetry Exporter 直连 Langfuse OTLP 端点

## Capabilities

### New Capabilities

- `multi-model-chat`: 多模型统一切换聊天能力，支持 DeepSeek / MiMo / DashScope 动态路由
- `rag-retrieval`: RAG 检索增强生成模块，含文档加载、向量化、检索、增强生成全链路
- `function-calling`: Function Calling 工具调用能力，演示外部工具注册与调用
- `graph-workflow`: 基于 Spring AI Alibaba Graph 的图编排工作流，支持顺序/并行/条件分支节点
- `langfuse-observability`: Langfuse 3 集成，OTel Trace/Generation/Span 全链路追踪与可视化

### Modified Capabilities

（无已有 capability 需要修改）

## Impact

- **新增依赖**: spring-ai、spring-ai-alibaba、langfuse-java-sdk / OTel Java Agent、向量数据库驱动（PgVector 或 SimpleVectorStore）
- **基础设施**: 需要 Langfuse 3 实例（自托管或 Langfuse Cloud）及 OTLP 端点配置
- **API**: 新增 `/chat`、`/rag`、`/function-call`、`/graph/workflow` 等 REST 端点
- **配置**: `application.yml` 新增多模型 API Key、Langfuse 凭据、OTel Exporter 配置
