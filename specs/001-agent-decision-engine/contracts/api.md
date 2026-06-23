# API 接口契约：Spring AI + Langfuse3 演示项目

**功能**: 001-springai-langfuse3-demo
**日期**: 2026-06-04

## 基础信息

- **基础路径**: `/api`
- **内容类型**: `application/json`（除非另有说明）
- **字符编码**: UTF-8

---

## 1. 多模型对话接口

### POST /api/chat

同步对话接口，返回完整回答。

**请求体**:
```json
{
  "message": "你好，请介绍一下自己",
  "model": "deepseek"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息内容 |
| model | String | 是 | 模型标识：deepseek/mimo/dashscope |

**响应体（200）**:
```json
{
  "content": "你好！我是一个AI助手...",
  "model": "deepseek",
  "chatId": "chat-abc123",
  "tokenUsage": {
    "promptTokens": 15,
    "completionTokens": 42,
    "totalTokens": 57
  }
}
```

**错误响应（400）**:
```json
{
  "error": "模型不可用",
  "detail": "MiMo 模型的 API 密钥未配置"
}
```

---

## 2. Flux 响应式流式接口

### GET /api/chat/flux/stream

以 Project Reactor Flux 方式流式返回回答。

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息内容 |
| model | String | 是 | 模型标识 |

**响应**: `Content-Type: text/event-stream`

```
data: {"content":"你好","model":"deepseek"}

data: {"content":"！我是","model":"deepseek"}

data: {"content":"AI助手","model":"deepseek"}

data: [DONE]
```

---

## 3. SSE 标准流式接口

### GET /api/chat/sse/stream

以标准 SSE 协议流式返回回答。

**请求参数**: 同 Flux 流式接口。

**响应**: `Content-Type: text/event-stream`

```
event: message
data: {"content":"你好","model":"deepseek"}

event: message
data: {"content":"！我是","model":"deepseek"}

event: done
data: [DONE]
```

---

## 4. SseEmitter 流式接口

### GET /api/chat/emitter/stream

以 Spring MVC SseEmitter 方式流式返回回答。

**请求参数**: 同 Flux 流式接口。

**响应**: `Content-Type: text/event-stream`

```
event: chunk
data: {"content":"你好","model":"deepseek"}

event: chunk
data: {"content":"！我是","model":"deepseek"}

event: complete
data: {"status":"done"}
```

---

## 5. RAG 知识库问答接口

### POST /api/rag/ask

基于知识库的 RAG 问答（同步）。

**请求体**:
```json
{
  "message": "产品的核心功能有哪些",
  "model": "deepseek"
}
```

**响应体（200）**:
```json
{
  "content": "根据知识库文档，产品的核心功能包括...",
  "model": "deepseek",
  "sources": ["doc-001", "doc-002"]
}
```

### GET /api/rag/ask/stream

RAG 问答流式版本。

**请求参数**: 同 Flux 流式接口。
**响应**: `text/event-stream`，格式同 Flux 流式接口。

### POST /api/rag/ingest

摄入文档到知识库。

**请求体** (`multipart/form-data`):
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | File | 是 | 要摄入的文档文件（.txt/.pdf/.md） |

**响应体（200）**:
```json
{
  "status": "success",
  "documentId": "doc-003",
  "chunks": 12,
  "message": "文档已成功摄入知识库"
}
```

---

## 6. Function Calling 接口

### POST /api/tools/chat

带工具调用的对话接口。

**请求体**:
```json
{
  "message": "北京今天天气如何",
  "model": "deepseek"
}
```

**响应体（200）**:
```json
{
  "content": "北京今天天气晴朗，气温 25°C。",
  "model": "deepseek",
  "toolCalls": [
    {
      "toolName": "weatherFunction",
      "input": {"location": "北京"},
      "output": {"temp": 25, "unit": "°C", "condition": "晴"}
    }
  ]
}
```

---

## 7. Graph 工作流接口

### POST /api/graph/execute

执行 Graph 多步骤工作流。

**请求体**:
```json
{
  "input": "今天天气真不错，适合出去玩",
  "model": "dashscope"
}
```

**响应体（200）**:
```json
{
  "output": "这是一段关于生活休闲的文本...",
  "nodes": [
    {"name": "classify", "status": "completed", "durationMs": 120},
    {"name": "process", "status": "completed", "durationMs": 350},
    {"name": "output", "status": "completed", "durationMs": 80}
  ],
  "branch": "lifestyle"
}
```

---

## 8. 可观测性接口

### GET /api/observability/health

检查 Langfuse 连接状态。

**响应体（200）**:
```json
{
  "langfuseMode": "cloud",
  "status": "connected",
  "otelExporter": "otlp-http"
}
```

---

## 9. Skills Agent 接口（新增）

### POST /api/skills/chat

Skills Agent 对话接口，支持技能选择和多轮对话。

**请求体**:
```json
{
  "message": "分析一下A股市场",
  "threadId": "optional-thread-id"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息内容 |
| threadId | String | 否 | 对话会话标识，用于多轮对话 |

**响应体（200）**:
```json
{
  "reply": "根据市场分析技能，A股市场当前呈现...",
  "threadId": "thread-abc123",
  "durationMs": 2500
}
```

**技能选择示例**:
- "分析一下A股市场" → 加载 market-analysis 技能
- "帮我评估投资风险" → 加载 risk-assessment 技能
- "怎么配置投资组合" → 加载 portfolio-optimization 技能
- "推荐一些股票" → 加载 investment-recommendation 技能
- "北京天气怎么样" → 加载 weather-assistant 技能

### GET /api/skills

列出所有可用技能。

**响应体（200）**:
```json
{
  "registryType": "Classpath",
  "skillCount": 7,
  "skills": {
    "market-analysis": "市场分析技能。当用户询问市场趋势、大盘走势、行业分析、市场情绪等问题时使用此技能。",
    "risk-assessment": "风险评估技能。当用户询问投资风险、VaR 计算、风险控制等问题时使用此技能。",
    "portfolio-optimization": "投资组合优化技能。当用户询问资产配置、组合优化、再平衡等问题时使用此技能。",
    "investment-recommendation": "投资推荐技能。当用户询问投资建议、买什么股票、投资策略等问题时使用此技能。",
    "weather-assistant": "天气查询助手。当用户询问天气、气温、穿衣建议、出行建议等天气相关问题时使用此技能。",
    "java-spring-expert": "Java/Spring 专家。当用户询问 Java 或 Spring 相关技术问题时使用此技能。",
    "code-reviewer": "代码审查助手。当用户提交代码需要审查时使用此技能。"
  },
  "explanation": "Skills 使用渐进式披露：系统提示仅包含技能名称和描述，Agent 按需调用 read_skill(name) 加载完整内容"
}
```

---

## 10. 投资工具接口（新增）

### POST /api/tools/chat（增强）

带投资工具调用的对话接口。

**请求体**:
```json
{
  "message": "查询苹果公司的股价",
  "modelName": "deepSeekChatModel"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息内容 |
| modelName | String | 否 | 模型名称，默认 deepSeekChatModel |

**响应体（200）**:
```json
{
  "content": "苹果公司（AAPL）当前股价为 178.52 美元，较昨日上涨 2.35 美元，涨幅 1.33%。",
  "model": "deepSeekChatModel",
  "tokenUsage": {
    "promptTokens": 150,
    "completionTokens": 85,
    "totalTokens": 235
  }
}
```

**投资工具调用示例**:

#### 股价查询
```bash
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "查询苹果公司的股价"}'
```

#### 市场指数查询
```bash
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "查询上证指数"}'
```

#### 风险计算
```bash
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我计算60%股票、30%债券、10%现金的组合收益"}'
```

#### VaR 计算
```bash
curl -X POST http://localhost:8080/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "我投资了10万元，帮我计算95%置信水平下30天的VaR"}'
```

**投资工具列表**:

| 工具名称 | 功能 | 使用场景 |
|---------|------|---------|
| getStockPrice | 查询股票实时价格 | 用户询问股票价格 |
| getStockHistory | 查询股票历史价格 | 用户询问历史走势 |
| calculateReturn | 计算投资收益率 | 用户计算收益 |
| getMarketIndex | 查询大盘指数 | 用户询问大盘情况 |
| getMarketVolatility | 查询市场波动率 | 用户询问市场风险 |
| getMarketSentiment | 查询市场情绪 | 用户询问市场情绪 |
| getSectorPerformance | 查询行业板块 | 用户询问行业表现 |
| calculatePortfolioReturn | 计算投资组合收益 | 用户配置投资组合 |
| calculateValueAtRisk | 计算 VaR | 用户评估风险 |
| calculateSharpeRatio | 计算夏普比率 | 用户评估风险调整收益 |

---

## 错误码约定

| HTTP 状态码 | 说明 |
|------------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误或模型不可用 |
| 408 | 模型响应超时 |
| 500 | 服务器内部错误 |
| 503 | 外部服务不可用（模型 API 或 Langfuse） |
