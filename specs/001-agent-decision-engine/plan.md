# 实施计划：Spring AI + Langfuse3 完整演示项目

**分支**: `001-springai-langfuse3-demo` | **日期**: 2026-06-04 | **规格**: [spec.md](spec.md)

**更新日期**: 2026-06-17

**输入**: 功能规格说明 `specs/001-agent-decision-engine/spec.md`

## 摘要

构建一个基于 Java 21 + Spring Boot 3 的 Spring AI 演示项目，集成
DeepSeek、MiMo、通义千问三个 AI 模型，实现 Flux/SSE/SseEmitter
三种流式输出、RAG 知识库问答、Function Calling 工具调用、
Spring AI Alibaba Graph 工作流，并通过 OpenTelemetry 将全链路追踪
数据上报至 Langfuse（支持云端和 Docker 本地两种部署模式）。

**新增功能**：
- Skills 框架：模块化技能注册和渐进式披露
- 投资工具：股价查询、市场指标、风险计算
- 系统提示词：AI 角色和行为定义
- 主流程测试：端到端冒烟测试

## 技术上下文

**语言/版本**: Java 21（LTS）

**主要依赖**:
- Spring Boot 3.4.x
- Spring AI 1.0.0（含 spring-ai-starter-model-deepseek、spring-ai-starter-model-openai）
- Spring AI Alibaba 1.0.0.4（DashScope 集成 + Graph 工作流 + Skills 框架）
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

**规模/范围**: 演示项目，10 个用户场景，约 15-20 个 REST 端点

## 宪章合规检查

*门禁：Phase 0 研究前必须通过。Phase 1 设计后重新检查。*

| 原则 | 状态 | 说明 |
|------|------|------|
| I. 多模型灵活切换 | ✅ 通过 | DeepSeek 原生 + MiMo OpenAI 兼容 + DashScope 原生 |
| II. 流式优先架构 | ✅ 通过 | 三种流式机制各有独立端点 |
| III. 可观测性驱动 | ✅ 通过 | OTel OTLP → Langfuse，云端+本地双模式 |
| IV. 测试先行 | ✅ 通过 | JUnit 5，mvn test 全量通过后提交 |
| V. AI 原生功能完备 | ✅ 通过 | RAG + Function Calling + Graph + Skills 各有独立模块 |
| VI. 双模式部署 | ✅ 通过 | application-cloud.yml + application-local.yml |
| VII. 统一简体中文 | ✅ 通过 | 文档、注释、提交信息均使用简体中文 |

## 项目结构

### 文档（本功能）

```text
specs/001-agent-decision-engine/
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
├── src/main/java/com/zhou/ai/
│   ├── SpringAiDemoApplication.java
│   ├── common/
│   │   ├── config/
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
│   │       └── ChatService.java             # 对话服务层（含系统提示词）
│   ├── rag/
│   │   ├── controller/
│   │   │   └── RagController.java           # RAG 问答端点
│   │   ├── service/
│   │   │   ├── RagService.java              # RAG 服务层（含系统提示词）
│   │   │   └── DocumentIngestionService.java # 文档摄入服务
│   │   └── config/
│   │       └── VectorStoreConfig.java       # 向量存储配置
│   ├── tools/
│   │   ├── controller/
│   │   │   └── ToolChatController.java      # Function Calling 端点
│   │   ├── service/
│   │   │   ├── ToolChatService.java         # 工具调用服务（含系统提示词）
│   │   │   ├── WeatherToolService.java      # @Tool 天气查询工具
│   │   │   ├── CalculatorToolService.java   # @Tool 数学计算工具
│   │   │   ├── StockPriceToolService.java   # @Tool 股价查询工具（新增）
│   │   │   ├── MarketIndexToolService.java  # @Tool 市场指标工具（新增）
│   │   │   └── RiskCalculatorToolService.java # @Tool 风险计算工具（新增）
│   │   └── config/
│   │       └── ToolCallbackConfig.java      # ToolCallback 统一注册
│   ├── skills/                              # 新增模块
│   │   ├── controller/
│   │   │   └── SkillsAgentController.java   # Skills Agent 端点
│   │   ├── service/
│   │   │   └── SkillsAgentService.java      # Skills Agent 服务
│   │   └── config/
│   │       └── SkillsAgentConfig.java       # Skills Agent 配置
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
│       ├── filter/
│       │   └── ChatModelCompletionContentObservationFilter.java
│       └── controller/
│           └── ObservabilityController.java  # 可观测性健康检查
├── src/main/resources/
│   ├── application.yml
│   ├── application-cloud.yml
│   ├── application-local.yml
│   ├── docs/                                # RAG 示例文档
│   │   └── sample.txt
│   └── skills/                              # 新增：技能定义目录
│       ├── market-analysis/
│       │   └── SKILL.md                     # 市场分析技能
│       ├── risk-assessment/
│       │   └── SKILL.md                     # 风险评估技能
│       ├── portfolio-optimization/
│       │   └── SKILL.md                     # 投资组合优化技能
│       ├── investment-recommendation/
│       │   └── SKILL.md                     # 投资推荐技能
│       ├── weather-assistant/
│       │   └── SKILL.md                     # 天气助手技能
│       ├── java-spring-expert/
│       │   └── SKILL.md                     # Java/Spring 专家技能
│       └── code-reviewer/
│           └── SKILL.md                     # 代码审查技能
└── src/test/java/com/zhou/ai/
    ├── ApplicationIntegrationTest.java      # 主流程冒烟测试（新增）
    ├── skills/
    │   ├── SkillsAgentIntegrationTest.java  # Skills Agent 集成测试
    │   └── SkillsAgentServiceTest.java      # Skills Agent 单元测试
    ├── tools/
    │   ├── CalculatorToolServiceTest.java   # 计算器工具测试
    │   └── ToolChatServiceTest.java         # 工具调用服务测试
    ├── chat/
    │   └── ChatControllerTest.java
    ├── rag/
    │   └── RagServiceTest.java
    ├── graph/
    │   └── GraphWorkflowServiceTest.java
    └── common/
        └── ModelRouterTest.java
```

**结构决策**: 采用单体 Spring Boot 项目，按功能域（chat/rag/tools/skills/graph/
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
  ├── function-calling（依赖 common，含投资工具）
  ├── skills（依赖 common，使用 Spring AI Alibaba Skills）
  ├── graph-workflow（依赖 common，使用 Spring AI Alibaba Graph）
  └── observability（依赖 common，被其他模块间接使用）

并行度：
  - common 必须最先完成
  - chat-streaming / rag / function-calling / skills / graph-workflow 可并行开发
  - observability 可与功能模块并行开发（通过配置而非代码耦合）
```

---

## 新增功能模块

### 1. Skills 框架模块

**位置**: `src/main/java/com/zhou/ai/skills/`

**功能**:
- 技能自动注册（ClasspathSkillRegistry）
- 渐进式披露（系统提示词仅加载技能摘要）
- ReactAgent 集成
- 多轮对话支持

**端点**:
- `POST /api/skills/chat` - Skills Agent 对话
- `GET /api/skills` - 列出可用技能

**技能定义**:
- `skills/market-analysis/SKILL.md` - 市场分析技能
- `skills/risk-assessment/SKILL.md` - 风险评估技能
- `skills/portfolio-optimization/SKILL.md` - 投资组合优化技能
- `skills/investment-recommendation/SKILL.md` - 投资推荐技能

---

### 2. 投资工具模块

**位置**: `src/main/java/com/zhou/ai/tools/service/`

**新增工具**:

#### StockPriceToolService
- `getStockPrice(symbol)` - 查询股票实时价格
- `getStockHistory(symbol, days)` - 查询股票历史价格
- `calculateReturn(symbol, buyPrice, shares)` - 计算投资收益率

#### MarketIndexToolService
- `getMarketIndex(indexName)` - 查询大盘指数
- `getMarketVolatility()` - 查询市场波动率
- `getMarketSentiment()` - 查询市场情绪指标
- `getSectorPerformance(sector)` - 查询行业板块表现

#### RiskCalculatorToolService
- `calculatePortfolioReturn(stock, bond, cash)` - 计算投资组合收益
- `calculateValueAtRisk(amount, confidence, days)` - 计算 VaR
- `calculateSharpeRatio(return, riskFree, volatility)` - 计算夏普比率

---

### 3. 系统提示词模块

**位置**: 各 Service 类中

**系统提示词定义**:

#### ChatService
```java
private static final String SYSTEM_PROMPT = """
        你是一个智能投资顾问助手，专注于为用户提供专业的投资建议和财务分析。
        你需要：
        1. 理解用户的投资需求和风险偏好
        2. 基于真实数据和专业知识给出建议
        3. 说明投资风险，避免过度承诺
        4. 用通俗易懂的语言解释复杂的金融概念
        5. 如果不确定或信息不足，请说明需要进一步了解
        """;
```

#### RagService
```java
private static final String SYSTEM_PROMPT = """
        你是一个投资知识库助手，专门基于投资相关的文档和资料来回答用户的问题。
        你需要：
        1. 优先使用知识库中检索到的信息来回答
        2. 如果知识库中有相关信息，准确引用并说明来源
        3. 如果知识库中没有相关信息，请说明无法从知识库中找到，但仍可以提供一般性建议
        4. 对于财务数据和投资建议，务必声明这是基于知识库的参考信息，不构成投资建议
        5. 保持专业、客观、谨慎的态度
        """;
```

#### ToolChatService
```java
private static final String SYSTEM_PROMPT = """
        你是一个智能投资助手，拥有以下工具可以查询实时数据和进行计算：
        [工具列表和使用说明]
        使用工具的原则：
        1. 当用户询问股票相关信息时，优先使用股价查询工具获取实时数据
        2. 当用户询问大盘或市场整体情况时，使用市场指标工具
        3. 当用户需要计算收益、风险或优化配置时，使用风险计算工具
        4. 工具调用前先理解用户需求，调用后给出专业分析和建议
        5. 始终声明投资有风险，建议仅供参考
        """;
```

#### SkillsAgent
```java
.instruction("你是一个智能投资顾问助手，拥有多种专业技能。"
        + "根据用户的问题，先判断是否需要加载某个技能：\n"
        + "- market-analysis：市场分析\n"
        + "- risk-assessment：风险评估\n"
        + "- portfolio-optimization：投资组合优化\n"
        + "- investment-recommendation：投资推荐\n"
        + "- weather-assistant：天气查询\n"
        + "- java-spring-expert：Java/Spring专家\n"
        + "- code-reviewer：代码审查\n\n"
        + "如果需要，使用 read_skill 工具获取完整技能内容后再回答。\n"
        + "你也可以使用 queryWeather、calculate 和投资相关工具来辅助回答。\n"
        + "始终牢记：投资有风险，建议仅供参考，不构成投资建议。")
```

---

### 4. 主流程测试模块

**位置**: `src/test/java/com/zhou/ai/ApplicationIntegrationTest.java`

**测试流程**:

```
1. Skills 注册验证
   - 验证 7 个技能已注册
   ↓
2. Observability 健康检查
   - 验证 Langfuse 配置正确
   ↓
3. RAG 文档摄入
   - 摄入 sample.txt 文档
   ↓
4. RAG 问答
   - 基于文档内容问答
   ↓
5. Tools 工具调用
   - 股价查询、市场指标、风险计算
   ↓
6. Skills Agent 对话
   - 技能选择和多轮对话
   ↓
7. Graph 工作流
   - 条件分支和多步骤执行
   ↓
8. 多轮对话
   - 上下文保持验证
```

**测试策略**:
- 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 启动完整应用
- 使用 `TestRestTemplate` 进行 HTTP 调用
- 使用 `Assumptions.assumeTrue()` 在 API 不可用时优雅跳过
- 按 `@Order` 注解控制执行顺序

---

## Phase 0: 研究更新

### 研究主题 8：Skills 框架集成

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

### 研究主题 9：投资工具设计

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

### 研究主题 10：系统提示词管理

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

### 研究主题 11：主流程测试设计

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

## Phase 1: 设计更新

### 数据模型更新

新增实体：

#### 8. SkillMetadata — 技能元数据

代表一个注册到 SkillRegistry 的技能。

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 技能名称（如 market-analysis） |
| description | String | 技能描述 |
| content | String | 技能完整内容（SKILL.md） |
| path | String | 技能文件路径 |

**来源**: ClasspathSkillRegistry 自动扫描 `skills/` 目录

---

#### 9. InvestmentToolResult — 投资工具结果

代表投资工具调用的返回结果。

| 字段 | 类型 | 说明 |
|------|------|------|
| toolName | String | 工具名称 |
| symbol | String | 股票代码/指数名称（可选） |
| data | Map<String, Object> | 工具返回的数据 |
| timestamp | long | 调用时间戳 |

---

### API 契约更新

新增端点：

#### Skills Agent 端点

```yaml
POST /api/skills/chat
  请求体:
    message: String (必填)
    threadId: String (可选，用于多轮对话)
  响应:
    reply: String
    threadId: String
    durationMs: long

GET /api/skills
  响应:
    registryType: String
    skillCount: int
    skills: Map<String, String>
    explanation: String
```

---

### 快速启动更新

新增步骤：

```bash
# 7. 测试 Skills Agent
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "分析一下A股市场"}'

# 8. 测试投资工具
curl -X POST http://localhost:8182/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "查询苹果公司的股价"}'

# 9. 测试投资组合优化
curl -X POST http://localhost:8182/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我计算60%股票、30%债券、10%现金的组合收益"}'

# 10. 运行主流程测试
mvn test -Dtest=ApplicationIntegrationTest
```

---

## 实施阶段

### 阶段 1：基础设施（已完成）

- [x] 项目初始化和依赖配置
- [x] 多模型配置和路由
- [x] 异常处理和全局配置

### 阶段 2：核心功能（已完成）

- [x] 三种流式输出端点
- [x] RAG 知识库问答
- [x] Function Calling 工具调用
- [x] Graph 工作流
- [x] Langfuse 可观测性

### 阶段 3：投资功能（已完成）

- [x] 投资工具（股价、市场指标、风险计算）
- [x] Skills 框架（技能注册和渐进式披露）
- [x] 系统提示词（AI 角色定义）
- [x] 主流程测试

### 阶段 4：优化和文档（待完成）

- [ ] 文档完善
- [ ] 性能优化
- [ ] 部署指南
- [ ] 演示视频

---

## 验证清单

### 功能验证

- [ ] 多模型切换正常
- [ ] 三种流式输出正常
- [ ] RAG 问答准确
- [ ] Function Calling 调用成功
- [ ] Graph 工作流执行正确
- [ ] Skills 技能选择正确
- [ ] 投资工具返回数据
- [ ] Langfuse 追踪完整

### 代码质量

- [ ] 单元测试覆盖
- [ ] 集成测试通过
- [ ] 代码规范符合
- [ ] 文档完整

### 部署验证

- [ ] 本地环境启动
- [ ] 云端 Langfuse 连接
- [ ] Docker 部署正常
- [ ] API 密钥配置正确
