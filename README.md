# AgentDecisionEngine — 7节点投资决策团队求职

> 项目是一支由 **7 名 AI Agent 组成的投资决策团队**，精通意图理解、金融知识检索、市场数据采集、深度推演与决策生成。采用 **Java 21 虚拟线程 + Reactor Flux 并行调度**，最快 ~2 分钟完成从用户需求到投资报告的完整链路。现寻求一个有挑战性的业务场景施展才华。

## 👥 团队成员

| 编号 | 姓名 | 职位 | 擅长 |
|------|------|------|------|
| 1 | IntentClassifyAgent | 意图分类 | 两层递进分类（关键词白名单 + LLM 结构化分类），非投资消息直接婉拒，节省团队精力 |
| 2 | ProblemPerceptionAgent | 需求分析师 | 理解用户投资目标、预算、风险偏好、期限，输出结构化需求画像 |
| 3 | KnowledgeRetrievalAgent | 金融知识专家 | ReAct 工具调用，向量检索金融知识库，提供专业领域支撑 |
| 4 | DataFetchAgent | 市场数据专员 | ReAct 多工具调用（股价/指数/波动率/VaR/夏普比率），自动适配 Function Calling 模型 |
| 5 | ReasoningAnalysisAgent | 资深金融分析师 | 综合知识+数据，评估趋势/风险/收益，给出置信度标注 |
| 6 | DecisionGenerateAgent | 投资顾问 | 生成结构化投资建议（策略/操作/风控/期限），严格执行风险声明 |
| 7 | GraphScheduleAgent | 流程主管 | 汇总验证各成员输出，一致性评估，签署最终报告 |

### 协作关系

```
用户需求
  │                 ←── 【开启虚拟线程】
  ├── [1. 前台] 意图分类 ←── 投资? 
  │         │
  │         ✔ (进入团队)
  │         │
  │   [2. 需求分析师] 问题感知
  │         │
  │    ┌────┴────┐  ←── 【虚拟线程并行】
  │    │         │
  │  [3. 知识专家] [4. 数据专员]
  │   RAG检索    市场数据
  │    │         │
  │    └────┬────┘
  │         │
  │   [5. 资深分析师] 综合推演
  │         │
  │   [6. 投资顾问] 决策生成
  │         │
  │   [7. 流程主管] 汇总签署
  │         │
  │    ┌────┴────┐
  │    │  最终报告 + Langfuse 审计追踪
  │    └─────────┘
  │
  └── ✘ (非投资) → "我是投资助手，请咨询投资理财问题"
```

## 🚀 专业技能

### 多模型适配
- **DeepSeek**：主力推理 + Function Calling
- **OpenAI 兼容**：Agnes AI 等代理
- **DashScope Qwen**：中文优化场景
- **智能路由**：工具调用自动切换到支持 Function Calling 的模型，避免上游 404

### 工具调用 (Function Calling)
- `getStockPrice(symbol)` — 实时股价
- `getMarketIndex(indexName)` — 大盘指数
- `getMarketVolatility()` / `getMarketSentiment()` — 市场情绪
- `calculateValueAtRisk(amount, confidence, days)` — VaR 计算
- `calculateSharpeRatio(return, riskFree, volatility)` — 夏普比率
- `calculatePortfolioReturn(stock, bond, cash)` — 组合收益
- `retrieveKnowledge(query)` — 向量知识检索

### 流式 SSE 推送
每一步进展实时推送给前端（打字机效果），用户无需等待全部完成就能看到团队工作进程。

### 全链路可观测 (Langfuse 3.x)
- 每个团队成员的推理过程可追溯
- Token 用量 + 成本实时追踪
- 支持本地/云端双模式部署

## 🏗️ 技术功底

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.7 | 应用框架 |
| Spring AI | 1.1.2 | AI 模型抽象层 |
| Spring AI Alibaba | 1.1.2.2 | ReactAgent + Graph + DashScope |
| **Java 21 Virtual Threads** | — | **全局虚拟线程，零池化限制** |
| Reactor Flux | — | 响应式流 + Flux.merge 并行调度 |
| Langfuse | 3.x | 可观测性平台 |
| OpenTelemetry | 2.17.0 | Trace 导出 |
| Knife4j | 4.5.0 | API 文档 |

### 并发模型亮点

```
Tomcat 请求线程 (Virtual Thread) ← spring.threads.virtual.enabled=true
  │
  ├─ Step 1 (意图分类)     → 虚拟线程 #1
  ├─ Step 2 (问题感知)     → 虚拟线程 #2
  ├─ Step 3 (知识检索) ┐
  │                     ├─ Flux.merge() 并行 → 虚拟线程 #3 + #4
  ├─ Step 4 (数据获取) ┘
  ├─ Step 5 (推理分析)     → 虚拟线程 #5
  ├─ Step 6 (决策生成)     → 虚拟线程 #6
  └─ Step 7 (汇总输出)     → 虚拟线程 #7
```

- **全局**：`spring.threads.virtual.enabled=true` 自动配置 Tomcat + `@Async` + `TaskExecutor` 为虚拟线程
- **步骤执行**：`Executors.newVirtualThreadPerTaskExecutor()` → Reactor Scheduler，每个阻塞 LLM 调用独立虚拟线程
- **Steps 3+4 并行**：`Flux.merge()` 替代串行 `Flux.concat()`，知识检索与数据获取并发，节省约 30-45 秒
- **线程安全**：`ConcurrentHashMap` 承载多 Agent 共享状态

## ⚡ 快速体验

### 环境要求
- Java 21+
- Docker（Langfuse 本地追踪）
- Maven 3.9+

### 启动

```bash
# 1. 启动 Langfuse 追踪
docker compose -f docker/docker-compose-langfuse.yml up -d

# 2. 配置 API Key（application-dev.yml）
spring.ai.deepseek.api-key=你的key
spring.ai.openai.api-key=你的key

# 3. 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 面试我们（调 API）

```bash
# 流式决策（看我们团队实时工作）
curl -X POST http://localhost:8182/api/agent/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message":"我想投资科技股，预算10万，风险中等"}'

# 健康检查
curl http://localhost:8182/api/agent/health
```

观察 Steps 3+4 的 stepStart 事件几乎同时到达，确认**并行执行**生效。

访问 `http://localhost:8182/swagger-ui.html` 查看完整 API 文档。

## 📂 项目结构

```
src/main/java/com/zhou/ai/
├── agent/
│   ├── controller/MultiAgentController.java    # REST 入口
│   ├── service/MultiAgentInvestService.java    # 7 节点 Flux 编排 + 虚拟线程调度
│   ├── node/                                   # 7 个 Agent 节点
│   │   ├── IntentClassifyAgent.java
│   │   ├── ProblemPerceptionAgent.java
│   │   ├── KnowledgeRetrievalAgent.java
│   │   ├── DataFetchAgent.java
│   │   ├── ReasoningAnalysisAgent.java
│   │   ├── DecisionGenerateAgent.java
│   │   └── GraphScheduleAgent.java
│   ├── config/
│   │   ├── ReactAgentFactory.java              # Agent 工厂 + 模型路由
│   │   └── MultiChatClientConfig.java
│   └── model/
│       ├── AgentGraphState.java                # 状态键常量
│       └── AgentPipelineContext.java            # 上下文记录（空值兜底）
├── investment/                                 # 遗留流程（另一个团队）
├── observability/                              # Langfuse 追踪
├── graph/                                      # Graph 工作流
├── skills/                                     # Skills 技能框架
└── tools/                                      # 投资工具集
```

## 🔧 配置

| 环境变量 | 说明 |
|----------|------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 |
| `AGNES_AI_API_KEY` | OpenAI 兼容 API 密钥 |
| `DASHSCOPE_API_KEY` | DashScope API 密钥 |

Spring Profiles: `local` (本地Langfuse), `cloud` (云端Langfuse), `dev` (开发密钥, gitignore)

## 📄 许可

MIT License

## 相关资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Langfuse 文档](https://langfuse.com/docs)
- [DashScope 文档](https://dashscope.console.aliyun.com/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
