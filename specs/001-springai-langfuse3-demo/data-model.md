# 数据模型：Spring AI + Langfuse3 演示项目

**功能**: 001-springai-langfuse3-demo
**日期**: 2026-06-04

## 实体定义

### 1. AiModelConfig — AI 模型配置

代表一个 AI 模型提供者的连接配置。

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 模型显示名称（如"DeepSeek"） |
| provider | String | 提供商标识（deepseek/mimo/dashscope） |
| apiKey | String | API 密钥引用（从环境变量读取） |
| baseUrl | String | API 端点地址 |
| modelName | String | 模型标识符（如 deepseek-chat） |
| enabled | boolean | 是否已配置且可用 |

**实例**: 系统中存在三个实例——DeepSeek、MiMo、DashScope。

**验证规则**:
- `apiKey` 不得为空（启动时校验）
- `baseUrl` 必须为合法 URL
- `enabled` 为 true 时 `apiKey` 不得为空

---

### 2. ChatRequest — 对话请求

代表一次用户发起的 AI 对话请求。

| 字段 | 类型 | 说明 |
|------|------|------|
| message | String | 用户输入的消息内容 |
| model | String | 目标模型标识（deepseek/mimo/dashscope） |
| chatId | String | 对话会话标识（可选，用于多轮对话） |

**验证规则**:
- `message` 不得为空
- `model` 必须为已注册的模型标识之一

---

### 3. ChatResponse — 对话响应

代表 AI 模型的回答结果。

| 字段 | 类型 | 说明 |
|------|------|------|
| content | String | 回答内容 |
| model | String | 实际使用的模型名称 |
| chatId | String | 对话会话标识 |
| timestamp | long | 响应时间戳 |
| tokenUsage | TokenUsage | Token 用量信息 |

---

### 4. TokenUsage — Token 用量

| 字段 | 类型 | 说明 |
|------|------|------|
| promptTokens | int | 提示词 Token 数 |
| completionTokens | int | 补全 Token 数 |
| totalTokens | int | 总 Token 数 |

---

### 5. KnowledgeDocument — 知识库文档

代表一份被摄入 RAG 知识库的本地文档。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | String | 文档唯一标识 |
| filePath | String | 原始文件路径 |
| content | String | 文档原始内容 |
| chunks | List<Document> | 分割后的文档片段 |
| ingestedAt | long | 摄入时间戳 |
| status | enum | PENDING/INGESTED/FAILED |

**状态流转**: PENDING → INGESTED（成功摄入向量库）
PENDING → FAILED（摄入失败）

---

### 6. ToolDefinition — 工具定义

代表一个可被 Function Calling 调用的外部工具。

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 工具名称（如 weatherFunction） |
| description | String | 工具功能描述（供模型理解） |
| inputType | Class<?> | 输入参数类型 |
| outputType | Class<?> | 输出结果类型 |

---

### 7. TraceRecord — Trace 记录

代表一次请求在 Langfuse 中的追踪链路（只读视图）。

| 字段 | 类型 | 说明 |
|------|------|------|
| traceId | String | 追踪唯一标识 |
| spanName | String | 跨度名称（如 chat/completion） |
| modelName | String | 使用的模型 |
| promptTokens | int | 提示词 Token 数 |
| completionTokens | int | 补全 Token 数 |
| durationMs | long | 耗时（毫秒） |
| status | enum | OK/ERROR |

---

## 实体关系

```
AiModelConfig (3 instances)
    │
    ├── ChatRequest ──→ ChatResponse
    │                       └── TokenUsage
    │
    ├── KnowledgeDocument ──→ VectorStore (SimpleVectorStore)
    │
    ├── ToolDefinition ──→ Function Bean
    │
    └── TraceRecord (Langfuse, 只读)
```

**说明**:
- `AiModelConfig` 是所有功能的配置基础
- `ChatRequest`/`ChatResponse` 是对话流的核心数据对象
- `KnowledgeDocument` 管理 RAG 文档的摄入生命周期
- `ToolDefinition` 描述 Function Calling 的工具元数据
- `TraceRecord` 是 Langfuse 中追踪数据的客户端视图
