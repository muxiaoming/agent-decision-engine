# AgentDecisionEngine

基于 Spring AI Alibaba + ReactAgent 的多模型智能投资决策引擎，采用 7 节点 Multi-Agent Graph 架构，实现从意图识别、问题感知、知识检索、数据获取、推理分析到投资建议生成的全流程自动化。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.5 | 应用框架 |
| Spring AI | 1.1.2 | AI 模型抽象层 |
| Spring AI Alibaba | 1.1.2.2 | DashScope 集成 + ReactAgent + Graph 工作流 |
| Langfuse | 3.x | AI 可观测性平台（本地/云端） |
| Java | 21 | 运行时（虚拟线程） |
| OpenTelemetry | 2.17.0 | 指标和追踪导出 |
| Knife4j | 4.5.0 | OpenAPI 文档 |

## 核心功能

### 🎯 多 Agent Graph 投资决策引擎

本项目构建了一个**基于 Multi-Agent Graph 架构的投资决策系统**，7 个独立的 ReactAgent 节点各司其职，通过手动 `Flux.concat()` 串联实现真正的逐节点流式推送（打字机效果）。

### 📊 决策链路

```
用户投资需求
     ↓
┌─────────────────────────────────────┐
│  1. IntentClassifyAgent (意图分类)   │
│     两层递进：关键词匹配 → LLM 分类   │
│     非投资消息直接返回，不执行后续步骤  │
├─────────────────────────────────────┤
│  2. ProblemPerceptionAgent (问题感知) │
│     理解用户需求、约束条件和风险偏好    │
├─────────────────────────────────────┤
│  3. KnowledgeRetrievalAgent (知识检索)│
│     ReAct 工具调用：向量检索金融知识   │
│     模型自动切换 DeepSeek (Function Calling) │
├─────────────────────────────────────┤
│  4. DataFetchAgent (数据获取)        │
│     ReAct 多工具调用：股价/指数/风险   │
│     NonTransientAiException 降级处理  │
├─────────────────────────────────────┤
│  5. ReasoningAnalysisAgent (推理分析)│
│     分析风险收益、识别投资机会         │
├─────────────────────────────────────┤
│  6. DecisionGenerateAgent (决策生成) │
│     生成结构化投资建议和风险提示       │
├─────────────────────────────────────┤
│  7. GraphScheduleAgent (汇总输出)    │
│     汇总验证各步骤结果，输出最终报告    │
└─────────────────────────────────────┘
     ↓
  投资决策报告 + Langfuse 全链路追踪
```

### 🤖 多模型代理

- **支持模型**: DeepSeek、OpenAI 兼容、DashScope Qwen
- **智能模型路由**: 需要 Function Calling 的节点（知识检索、数据获取）自动切换到 DeepSeek（`resolveToolModel()`），避免部分 OpenAI 兼容代理在多工具定义场景下返回 404
- **流式输出**: 基于 `Flux.concat()` + `Flux.defer()` + `Schedulers.boundedElastic()` 实现真正的逐节点推送，前端获得打字机效果
- **降级策略**: 上游模型不可用（NonTransientAiException）时自动填充参考数据，不中断决策流程

### 🔧 投资工具集 (Function Calling)

- **股价查询工具**: 获取实时股票价格和历史数据
- **市场指标工具**: 获取市场整体指标（指数、波动率等）
- **风险计算器**: 计算投资组合的风险指标
- **知识检索工具**: 向量语义检索金融知识库
- **自动装配**: 工具自动注册到 ReactAgent 节点

### 📊 Langfuse 可观测性

- **全链路追踪**: 从用户需求到投资建议的完整追踪
- **决策审计**: 记录每个 Agent 节点的推理过程
- **成本监控**: 追踪 token 用量、响应时间、成功率
- **调试模式**: 支持本地和云端 Langfuse 双模式
- **健康检查**: `/api/observability/health` 验证连接状态

### 🧩 流式推送机制

采用手动 `Flux.concat()` 链替代 `StateGraph.compile().invoke()` 的原因：

`CompiledGraph.stream()` 虽返回 `Flux<NodeOutput>`，但每个 Agent 的 `AsyncNodeAction` 内部同步阻塞等待 LLM 响应后返回已完成 Future，导致 GraphRunner 看到所有节点瞬间完成，所有 NodeOutput 几乎同时 emit，前端无法获得打字机效果。

**解决方案**：手动 `Flux.concat()` 串联各节点的 `AsyncNodeAction`，配合 `Flux.defer()` 确保阻塞 LLM 调用在上一步事件推送完成之后才订阅执行，`Schedulers.boundedElastic()` 将阻塞调用踢出 NIO 线程。

```
Flux.concat(
  step1: intentClassify,
  Flux.defer(() -> {        // 仅当 step1 完成后才订阅
    if (非投资) return completeEvent;
    return Flux.concat(
      step2: problemPerception,
      step3: knowledgeRetrieval,
      step4: dataFetch,
      step5: reasoningAnalysis,
      step6: decisionGenerate,
      step7: graphSchedule,
      decisionComplete
    );
  })
)
```

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  Client (Web App / API / curl)                             │
└──────────┬──────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────┐
│ MultiAgentController │  ← /agent/decide/stream (SSE)
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────────┐
│ MultiAgentInvestService                                      │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ Flux.concat() 7-Node Pipeline                            │ │
│ │                                                          │ │
│ │  1. IntentClassifyAgent    → 意图分类 + 非投资过滤        │ │
│ │  2. ProblemPerceptionAgent → 问题理解                     │ │
│ │  3. KnowledgeRetrievalAgent → ReAct 知识检索 (DeepSeek)  │ │
│ │  4. DataFetchAgent         → ReAct 数据获取 (DeepSeek)   │ │
│ │  5. ReasoningAnalysisAgent → 推理分析                     │ │
│ │  6. DecisionGenerateAgent  → 决策生成                     │ │
│ │  7. GraphScheduleAgent     → 汇总输出                     │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ ReactAgentFactory (Agent 工厂)                           │ │
│ │ ├── resolveModel()      → 普通模型解析                    │ │
│ │ ├── resolveToolModel()  → 工具调用自动切 DeepSeek         │ │
│ │ └── 7 个 Agent Builder   → 统一构建和管理                 │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ ToolCallbackProvider (工具集)                             │ │
│ │ ├── StockPriceTool, MarketIndexTool, RiskCalculator      │ │
│ │ └── KnowledgeRetrievalTool                               │ │
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│ RAG    │ │Langfuse│  ← 知识检索 + 全链路追踪
└────────┘ └────────┘
```

## 快速启动

### 前提条件
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. 克隆和构建

```bash
git clone https://github.com/your-repo/agent-decision-engine.git
cd agent-decision-engine
mvn clean install -DskipTests
```

### 2. 启动 Langfuse

```bash
docker compose -f docker/docker-compose-langfuse.yml up -d
```

访问 `http://localhost:3000` 完成 Langfuse 初始化。

### 3. 配置 API 密钥

创建 `src/main/resources/application-dev.yml`：

```yaml
spring:
  ai:
    deepseek:
      api-key: your-deepseek-key
    openai:
      api-key: your-openai-compatible-key
    dashscope:
      api-key: your-dashscope-key

otel:
  exporter:
    otlp:
      headers:
        Authorization: "Basic your-langfuse-credentials"
```

### 4. 启动应用

```bash
# 使用 dev 配置启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

应用启动后访问:
- **API 文档**: `http://localhost:8182/swagger-ui.html`
- **健康检查**: `http://localhost:8182/agent/health`

### 5. 测试多 Agent 决策 API

**流式决策请求（SSE）**
```bash
curl -X POST http://localhost:8182/agent/decide/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我想投资科技股，风险承受能力中等，预算 10 万",
    "modelName": "openAiChatModel"
  }'
```

**GET 方式流式请求**
```bash
curl "http://localhost:8182/agent/decide/stream?message=分析一下AI行业的投资机会&modelName=openAiChatModel"
```

**健康检查**
```bash
curl http://localhost:8182/agent/health
```

## 配置选项

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | - |
| `OPENAI_API_KEY` | OpenAI 兼容 API 密钥 | - |
| `DASHSCOPE_API_KEY` | DashScope API 密钥 | - |
| `LANGFUSE_MODE` | Langfuse 模式 (local/cloud) | `local` |

### Spring Profiles

| Profile | 说明 |
|---------|------|
| `local` | 本地 Langfuse 部署 |
| `cloud` | 云端 Langfuse |
| `dev` | 开发配置（不提交到 git） |

### 模型路由策略

| Agent 节点 | 模型策略 | 说明 |
|-----------|---------|------|
| IntentClassifyAgent | `resolveModel()` | 普通 LLM 调用 |
| ProblemPerceptionAgent | `resolveModel()` | 普通 LLM 调用 |
| KnowledgeRetrievalAgent | `resolveToolModel()` → DeepSeek | 需要 Function Calling |
| DataFetchAgent | `resolveToolModel()` → DeepSeek | 需要多工具调用 |
| ReasoningAnalysisAgent | `resolveModel()` | 普通 LLM 调用 |
| DecisionGenerateAgent | `resolveModel()` | 普通 LLM 调用 |
| GraphScheduleAgent | `resolveModel()` | 普通 LLM 调用 |

## 文档

- **功能规格**: `specs/001-agent-decision-engine/spec.md`
- **实现计划**: `specs/001-agent-decision-engine/plan.md`
- **数据模型**: `specs/001-agent-decision-engine/data-model.md`
- **API 契约**: `specs/001-agent-decision-engine/contracts/api.md`
- **快速启动**: `specs/001-agent-decision-engine/quickstart.md`

## 开发指南

### 添加新 Agent 节点

1. 在 `node/` 包中创建新的 Agent 类
2. 实现 `asNodeAction()` 方法返回 `AsyncNodeAction`
3. 在 `ReactAgentFactory` 中添加 Agent 构建方法
4. 在 `MultiAgentInvestService` 中注入并添加到 `Flux.concat()` 链

### 添加新工具

1. 创建 `@Component` 类并使用 `@Tool` 注解
2. 工具自动注册到 `ToolCallbackProvider`
3. 在 `ReactAgentFactory` 中通过 `.methodTools()` 注册到对应 Agent

### 集成新的 Langfuse 特性

参考 Langfuse 3.x 文档和 `observability` 包中的实现。

## 故障排除

### 连接 Langfuse 失败

```bash
# 检查 Langfuse 是否运行
docker compose -f docker/docker-compose-langfuse.yml ps

# 验证健康状态
curl http://localhost:8182/api/observability/health
```

### 工具调用返回 404

如果使用 OpenAI 兼容代理（如 Agnes AI），多工具调用可能返回 404。系统已自动将 KnowledgeRetrievalAgent 和 DataFetchAgent 切换到 DeepSeek 模型。

### 向量存储数据丢失

`SimpleVectorStore` 存储在内存中，重启应用会丢失数据。切换到生产向量存储：

- **Chroma**: `spring-ai-starter-vector-store-chroma`
- **Milvus**: `spring-ai-starter-vector-store-milvus`
- **PostgreSQL**: `spring-ai-starter-vector-store-pgvector`

## 贡献

欢迎通过 Issue 和 Pull Request 贡献代码。

## 许可

MIT License

## 相关资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Langfuse 文档](https://langfuse.com/docs)
- [DashScope 文档](https://dashscope.console.aliyun.com/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
