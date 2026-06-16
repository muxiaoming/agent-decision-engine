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

## 错误码约定

| HTTP 状态码 | 说明 |
|------------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误或模型不可用 |
| 408 | 模型响应超时 |
| 500 | 服务器内部错误 |
| 503 | 外部服务不可用（模型 API 或 Langfuse） |
