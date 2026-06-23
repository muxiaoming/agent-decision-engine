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

---

### 8. SkillMetadata — 技能元数据

代表一个注册到 SkillRegistry 的技能。

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 技能名称（如 market-analysis） |
| description | String | 技能描述 |
| content | String | 技能完整内容（SKILL.md） |
| path | String | 技能文件路径 |

**来源**: ClasspathSkillRegistry 自动扫描 `skills/` 目录

**验证规则**:
- `name` 不得为空，必须符合 kebab-case 格式
- `description` 不得为空
- `content` 必须包含有效的 Markdown 内容

**已注册技能**:
- `market-analysis` - 市场分析技能
- `risk-assessment` - 风险评估技能
- `portfolio-optimization` - 投资组合优化技能
- `investment-recommendation` - 投资推荐技能
- `weather-assistant` - 天气助手技能
- `java-spring-expert` - Java/Spring 专家技能
- `code-reviewer` - 代码审查技能

---

### 9. InvestmentToolResult — 投资工具结果

代表投资工具调用的返回结果。

| 字段 | 类型 | 说明 |
|------|------|------|
| toolName | String | 工具名称 |
| symbol | String | 股票代码/指数名称（可选） |
| data | Map<String, Object> | 工具返回的数据 |
| timestamp | long | 调用时间戳 |

**工具列表**:
- `getStockPrice` - 股价查询
- `getStockHistory` - 历史价格查询
- `calculateReturn` - 收益率计算
- `getMarketIndex` - 市场指数查询
- `getMarketVolatility` - 波动率查询
- `getMarketSentiment` - 市场情绪查询
- `getSectorPerformance` - 行业板块查询
- `calculatePortfolioReturn` - 投资组合收益计算
- `calculateValueAtRisk` - VaR 计算
- `calculateSharpeRatio` - 夏普比率计算

---

### 10. SystemPrompt — 系统提示词

代表 AI 助手的角色和行为定义。

| 字段 | 类型 | 说明 |
|------|------|------|
| service | String | 服务名称（ChatService/RagService 等） |
| role | String | 角色定位（投资顾问/知识库助手等） |
| instructions | String | 行为指南和约束 |

**已定义提示词**:
- `ChatService` - 智能投资顾问助手
- `RagService` - 投资知识库助手
- `ToolChatService` - 投资工具使用指南
- `SkillsAgent` - 技能选择和工具使用指南

---

## 实体关系（更新）

```
AiModelConfig (3 instances)
    │
    ├── ChatRequest ──→ ChatResponse
    │                       └── TokenUsage
    │
    ├── KnowledgeDocument ──→ VectorStore (SimpleVectorStore)
    │
    ├── ToolDefinition ──→ @Tool Bean
    │   ├── WeatherToolService
    │   ├── CalculatorToolService
    │   ├── StockPriceToolService (新增)
    │   ├── MarketIndexToolService (新增)
    │   └── RiskCalculatorToolService (新增)
    │
    ├── SkillMetadata ──→ ClasspathSkillRegistry (新增)
    │   ├── market-analysis
    │   ├── risk-assessment
    │   ├── portfolio-optimization
    │   ├── investment-recommendation
    │   ├── weather-assistant
    │   ├── java-spring-expert
    │   └── code-reviewer
    │
    ├── SystemPrompt ──→ 各 Service 类 (新增)
    │
    └── TraceRecord (Langfuse, 只读)
```

**说明**:
- `AiModelConfig` 是所有功能的配置基础
- `ChatRequest`/`ChatResponse` 是对话流的核心数据对象
- `KnowledgeDocument` 管理 RAG 文档的摄入生命周期
- `ToolDefinition` 描述 Function Calling 的工具元数据（新增投资工具）
- `SkillMetadata` 描述 Skills 框架的技能元数据（新增）
- `SystemPrompt` 定义 AI 助手的角色和行为（新增）
- `TraceRecord` 是 Langfuse 中追踪数据的客户端视图
