# AgentDecisionEngine

基于 Spring AI + Langfuse 3 的多模型智能投资代理决策引擎，实现从问题理解、知识检索、数据分析到投资建议的全流程自动化。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.5 | 应用框架 |
| Spring AI | 1.1.2 | AI 模型抽象层 |
| Spring AI Alibaba | 1.1.2.2 | DashScope 集成 + Graph 工作流 |
| Langfuse | 3.x | AI 可观测性平台（本地/云端） |
| Java | 21 | 运行时（虚拟线程） |
| OpenTelemetry | 2.17.0 | 指标和追踪导出 |
| Knife4j | 4.5.0 | OpenAPI 文档 |

## 核心功能

### 🎯 智能投资代理决策引擎

本项目构建了一个**投资领域的智能代理系统**，能够自主分析市场、理解用户需求、调用工具获取数据、生成投资建议。所有功能围绕**决策**这一核心目标设计，形成完整的决策链路。

### 📊 决策链路

```
用户投资需求
     ↓
┌─────────────────────────────────────┐
│  1. 问题感知 (AgentChat)            │
│     理解用户需求和约束条件            │
├─────────────────────────────────────┤
│  2. 知识检索 (RAG)                  │
│     检索财报、市场报告、投资策略      │
├─────────────────────────────────────┤
│  3. 数据获取 (Function)             │
│     查询实时股价、市场指标            │
├─────────────────────────────────────┤
│  4. 推理分析 (AgentReasoning)       │
│     分析风险收益、识别投资机会        │
├─────────────────────────────────────┤
│  5. 决策生成 (AgentDecision)        │
│     生成投资建议和风险提示            │
├─────────────────────────────────────┤
│  6. 流程编排 (Graph)                │
│     管理决策工作流（分析→优化→执行）  │
├─────────────────────────────────────┤
│  7. 追踪审计 (Langfuse)             │
│     记录决策过程，支持复盘和审计      │
└─────────────────────────────────────┘
     ↓
  投资决策报告 + 可观测性追踪
```

### 🤖 多模型代理
- **支持模型**: DeepSeek、Xiaomi MiMo、DashScope Qwen
- **动态切换**: 通过 `modelName` 参数实时切换模型
- **流式输出**: 支持 Flux/SSE 三种流式实现
- **智能路由**: 根据任务类型自动选择最适合的模型

### 🔍 RAG 投资知识库
- **知识库摄取**: 上传财报、市场报告自动分割并存入向量存储
- **语义搜索**: 基于向量相似度检索相关投资知识
- **问答增强**: 使用检索结果增强 LLM 回答准确性和专业性

### 🔧 投资工具集 (Function Calling)
- **股价查询工具**: 获取实时股票价格和历史数据
- **市场指标工具**: 获取市场整体指标（指数、波动率等）
- **风险计算器**: 计算投资组合的风险指标
- **自动装配**: 工具自动注册到所有模型客户端

### 📊 Langfuse 可观测性
- **全链路追踪**: 从用户需求到投资建议的完整追踪
- **决策审计**: 记录每个决策步骤的推理过程
- **成本监控**: 追踪 token 用量、响应时间、成功率
- **调试模式**: 支持本地和云端 Langfuse 双模式
- **健康检查**: `/api/observability/health` 验证连接状态

### 🎯 Graph 决策工作流
- **决策流程编排**: 有向图（DAG）管理投资决策流程
- **条件分支**: 支持基于风险偏好、市场情况的路由分支
- **状态管理**: 决策流程状态追踪和日志记录
- **检查点**: 支持决策流程的暂停、恢复和回滚

### 👨‍💼 Agent 技能框架 (Skills)
- **技能动态加载**: 基于描述的技能自动发现和注册
- **渐进式披露**: 系统仅暴露技能名称和描述，Agent 按需加载完整内容
- **可扩展架构**: 轻松添加新技能（投资分析、风险评估等）
- **ReactAgent 集成**: 使用 Spring AI Alibaba 的 ReactAgent 框架

#### 核心技能模块

| 技能 | 功能 | 在决策中的作用 |
|------|------|---------------|
| **MarketAnalysisSkill** | 市场分析技能 | 分析市场趋势、行业动态 |
| **RiskAssessmentSkill** | 风险评估技能 | 评估投资组合风险 |
| **PortfolioOptimizationSkill** | 投资组合优化技能 | 优化资产配置 |
| **InvestmentRecommendationSkill** | 投资推荐技能 | 生成投资建议 |

#### 技能架构

```
用户投资需求
     ↓
┌─────────────────────────────────────┐
│  AgentDecisionEngine               │
├─────────────────────────────────────┤
│  SkillRegistry (技能注册中心)       │
│  ├── 市场分析技能                   │
│  ├── 风险评估技能                   │
│  ├── 投资组合优化技能               │
│  └── 投资推荐技能                   │
├─────────────────────────────────────┤
│  ReactAgent (智能代理)             │
│  ├── 理解用户意图                   │
│  ├── 选择合适的技能                 │
│  ├── 调用技能获取结果               │
│  └── 整合结果生成建议               │
└─────────────────────────────────────┘
     ↓
  投资决策报告
```

**为什么使用 Skills？**

1. **模块化**: 每个技能独立封装，易于维护和测试
2. **可扩展**: 新增技能只需注册，无需修改核心代码
3. **智能选择**: Agent 根据用户意图自动选择最合适的技能
4. **渐进式加载**: 避免一次性加载所有技能内容，提高效率
5. **可观测性**: 每个技能调用在 Langfuse 中可追踪

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  Client (Web App / API / curl)                             │
└──────────┬──────────────────────────────────────────────────┘
           │
           ▼
┌──────────────────┐
│ InvestmentController  │  ← 接收投资决策请求
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│ AgentDecisionEngine                      │
│ ┌──────────────────────────────────────┐ │
│ │ ReactAgent (智能代理核心)            │ │
│ │ ├── 理解用户意图                     │ │
│ │ ├── 选择合适的技能和工具             │ │
│ │ ├── 调用技能/工具获取结果            │ │
│ │ └── 整合结果生成投资建议             │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ ┌──────────────────────────────────────┐ │
│ │ SkillRegistry (技能注册中心)         │ │
│ │ ├── MarketAnalysisSkill             │ │
│ │ ├── RiskAssessmentSkill             │ │
│ │ ├── PortfolioOptimizationSkill      │ │
│ │ └── InvestmentRecommendationSkill   │ │
│ └──────────────────────────────────────┘ │
│                                          │
│ ┌──────────────────────────────────────┐ │
│ │ ToolCallbackProvider (工具集)        │ │
│ │ ├── StockPriceTool                  │ │
│ │ ├── MarketIndexTool                 │ │
│ │ └── RiskCalculator                  │ │
│ └──────────────────────────────────────┘ │
└──────────────────────────────────────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌────────┐ ┌────────┐
│ RAG    │ │Graph   │  ← 知识检索 + 工作流编排
└────┬───┘ └────┬───┘
     │          │
     └────┬─────┘
          │
          ▼
    ┌──────────┐
    │ Langfuse │  ← 全链路追踪（包括技能调用）
    └──────────┘
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
      api-key: your-mimo-key
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
- **健康检查**: `http://localhost:8182/api/observability/health`

### 5. 测试投资决策 API

**投资决策请求**
```bash
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我想投资科技股，风险承受能力中等，预算 10 万",
    "riskLevel": "medium",
    "budget": 100000
  }'
```

**流式决策输出**
```bash
curl -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "分析一下 AI 行业的投资机会",
    "riskLevel": "high",
    "budget": 500000
  }'
```

**查询可用模型**
```bash
curl http://localhost:8182/api/models
```

**查询可用技能**
```bash
curl http://localhost:8182/api/skills/list
```

**投资知识库上传**
```bash
curl -X POST http://localhost:8182/api/rag/documents \
  -F "file=@market-report-2024.txt"
```

**查看决策追踪**
```bash
# 在 Langfuse 中查看决策追踪（包括技能调用）
curl http://localhost:8182/api/observability/health
```

**查看技能诊断信息**
```bash
curl http://localhost:8182/api/skills/diagnostics
```

## 配置选项

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | - |
| `MIMO_API_KEY` | Xiaomi MiMo API 密钥 | - |
| `DASHSCOPE_API_KEY` | DashScope API 密钥 | - |
| `LANGFUSE_MODE` | Langfuse 模式 (local/cloud) | `local` |

### Spring Profiles

| Profile | 说明 |
|---------|------|
| `local` | 本地 Langfuse 部署 |
| `cloud` | 云端 Langfuse |
| `dev` | 开发配置（不提交到 git） |

### 运行时配置

```bash
# 指定启动 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local

# 切换端口
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

## 文档

- **功能规格**: `specs/001-agent-decision-engine/spec.md`
- **实现计划**: `specs/001-agent-decision-engine/plan.md`
- **数据模型**: `specs/001-agent-decision-engine/data-model.md`
- **API 契约**: `specs/001-agent-decision-engine/contracts/api.md`
- **快速启动**: `specs/001-agent-decision-engine/quickstart.md`

## 开发指南

### 添加新模型

1. 在 `application.yml` 中添加模型配置
2. Spring AI 自动创建 `ChatModel` Bean
3. 自动被 `ModelRouter` 发现和注册

### 添加新工具

1. 创建 `@Component` 类并使用 `@Tool` 注解
2. 工具自动注册到 `ToolCallbackProvider`
3. 自动装配到所有模型客户端

### 集成新的 Agent 技能

1. 在 `skills/` 包中创建新的技能类
2. 使用 `@AgentSkill` 注解声明技能
3. Agent 自动发现和注册新技能

### 集成新的 Langfuse 特性

参考 Langfuse 3.x 文档和 `observability` 包中的实现。

## 故障排除

### 连接 Langfuse 失败

```bash
# 检查 Langfuse 是否运行
docker compose -f docker/docker-compose-langfuse.yml ps

# 查看应用日志
docker logs agent-decision-engine

# 验证健康状态
curl http://localhost:8182/api/observability/health
```

### 模型 API 密钥无效

```bash
# 检查环境变量
echo $DEEPSEEK_API_KEY

# 或查看 dev 配置
cat src/main/resources/application-dev.yml
```

### 向量存储数据丢失

`SimpleVectorStore` 存储在内存中，重启应用会丢失数据。切换到生产向量存储：

- **Chroma**: `spring-ai-starter-vector-store-chroma`
- **Milvus**: `spring-ai-starter-vector-store-milvus`
- **PostgreSQL**: `spring-ai-starter-vector-store-pgvector`

### 决策流程失败

```bash
# 查看 Graph 工作流执行日志
curl http://localhost:8182/api/graph/status

# 在 Langfuse 中追踪决策链路
# 访问 http://localhost:3000 查看 Trace
```

## 贡献

欢迎通过 Issue 和 Pull Request 贡献代码。

## 许可

MIT License

## 相关资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Langfuse 文档](https://langfuse.com/docs)
- [DashScope 文档](https://dashscope.console.aliyun.com/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
