## ADDED Requirements

### Requirement: 主流程冒烟测试

系统 SHALL 实现一个主流程冒烟测试，串联所有核心功能，验证系统端到端工作流程。

#### Scenario: 测试框架配置
- **WHEN** 测试类被创建
- **THEN** 使用 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` 启动完整应用
- **AND** 使用 `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` 确保测试顺序执行
- **AND** 使用 `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` 共享测试实例

#### Scenario: 测试依赖注入
- **WHEN** 测试类初始化
- **THEN** 通过 `@Autowired` 注入 `TestRestTemplate` 用于 HTTP 调用
- **AND** 通过 `@Autowired` 注入 `SkillsAgentService` 用于直接服务调用

### Requirement: 技能注册验证

测试 SHALL 首先验证所有投资技能已正确注册。

#### Scenario: 技能数量验证
- **WHEN** 测试执行技能注册检查
- **THEN** 验证 SkillRegistry 中注册了 7 个技能（market-analysis、risk-assessment、portfolio-optimization、investment-recommendation、weather-assistant、java-spring-expert、code-reviewer）

#### Scenario: 技能详情验证
- **WHEN** 测试查询技能列表
- **THEN** 验证每个技能的名称和描述可正确获取

### Requirement: RAG 文档摄入测试

测试 SHALL 验证 RAG 文档摄入功能。

#### Scenario: 文档摄入成功
- **WHEN** 测试发送文档到摄入端点
- **THEN** 验证 `POST /api/rag/ingest` 返回 200 状态码
- **AND** 验证响应包含摄入结果信息

#### Scenario: 测试文档准备
- **WHEN** 测试执行前
- **THEN** 确保 `src/main/resources/docs/sample.txt` 测试文档存在

### Requirement: RAG 问答测试

测试 SHALL 验证基于文档的问答功能。

#### Scenario: RAG 问答成功
- **WHEN** 测试发送 RAG 问答请求
- **THEN** 验证 `POST /api/rag/ask` 返回 200 状态码
- **AND** 验证响应包含基于文档内容的回答

#### Scenario: 系统提示词应用
- **WHEN** RAG 问答执行
- **THEN** 验证回答符合投资知识库助手角色定位

### Requirement: 工具调用测试

测试 SHALL 验证投资工具调用功能。

#### Scenario: 股价查询工具调用
- **WHEN** 测试发送股价查询相关问题
- **THEN** 验证 `POST /api/tools/chat` 返回 200 状态码
- **AND** 验证模型调用了股价查询工具
- **AND** 验证回答包含股价相关信息

#### Scenario: 市场指标工具调用
- **WHEN** 测试发送市场指标相关问题
- **THEN** 验证模型调用了市场指标工具
- **AND** 验证回答包含市场数据

#### Scenario: 风险计算工具调用
- **WHEN** 测试发送风险计算相关问题
- **THEN** 验证模型调用了风险计算工具
- **AND** 验证回答包含风险指标

### Requirement: Skills Agent 对话测试

测试 SHALL 验证 Skills Agent 的技能选择和对话功能。

#### Scenario: 技能自动选择
- **WHEN** 测试发送投资相关问题
- **THEN** 验证 ReactAgent 自动选择合适的投资技能

#### Scenario: 技能内容加载
- **WHEN** Agent 选择技能后
- **THEN** 验证通过 `read_skill` 工具加载了完整的技能内容

#### Scenario: 多轮对话上下文保持
- **WHEN** 测试使用相同 threadId 发送多轮请求
- **THEN** 验证第二轮回答能基于第一轮上下文进行追问

### Requirement: Graph 工作流测试

测试 SHALL 验证 Graph 工作流的执行功能。

#### Scenario: 工作流执行成功
- **WHEN** 测试发送 Graph 执行请求
- **THEN** 验证 `POST /api/graph/execute` 返回 200 状态码
- **AND** 验证响应包含分类结果和处理输出

#### Scenario: 条件分支路由
- **WHEN** 输入包含特定关键词（如"技术"、"编程"）
- **THEN** 验证工作流正确路由到对应处理节点

### Requirement: Observability 追踪测试

测试 SHALL 验证 Langfuse 可观测性功能。

#### Scenario: 健康检查
- **WHEN** 测试查询可观测性健康状态
- **THEN** 验证 `GET /api/observability/health` 返回 200 状态码
- **AND** 验证响应包含 langfuseMode 和 otelEndpoint 信息

#### Scenario: Trace 数据验证
- **WHEN** 执行 AI 请求后
- **THEN** 验证 Langfuse 中记录了完整的追踪数据

### Requirement: 多轮对话测试

测试 SHALL 验证多轮对话的上下文保持能力。

#### Scenario: 同一会话上下文
- **WHEN** 测试使用相同 threadId 发送多轮投资相关问题
- **THEN** 验证第二轮回答能理解第一轮的上下文

#### Scenario: 技能切换
- **WHEN** 测试在多轮对话中切换话题（如从天气切换到投资）
- **THEN** 验证 Agent 能正确切换到对应的技能

### Requirement: 测试数据管理

测试 SHALL 管理测试数据的准备和清理。

#### Scenario: 测试数据准备
- **WHEN** 测试开始前
- **THEN** 确保必要的测试文档存在

#### Scenario: 测试数据隔离
- **WHEN** 测试执行中
- **THEN** 使用独立的 threadId 避免与其他测试冲突

#### Scenario: 测试结果输出
- **WHEN** 测试执行完成
- **THEN** 输出关键结果信息到控制台，便于调试

### Requirement: 测试断言策略

测试 SHALL 使用适当的断言策略验证核心功能。

#### Scenario: 状态码验证
- **WHEN** 测试调用 API 端点
- **THEN** 验证 HTTP 状态码为 2xx

#### Scenario: 响应内容验证
- **WHEN** 测试收到 API 响应
- **THEN** 验证响应包含必需字段（如 content、traceId 等）

#### Scenario: 业务逻辑验证
- **WHEN** 测试验证业务功能
- **THEN** 使用具体断言验证业务结果（如技能数量、分类结果等）

#### Scenario: 优雅降级
- **WHEN** AI API 不可用时
- **THEN** 使用 `Assumptions.assumeTrue()` 跳过依赖外部服务的测试

### Requirement: 测试顺序控制

测试 SHALL 按照依赖关系顺序执行。

#### Scenario: 基础功能优先
- **WHEN** 测试开始执行
- **THEN** 先验证基础功能（技能注册、健康检查）

#### Scenario: 数据准备在前
- **WHEN** 测试 RAG 功能
- **THEN** 先执行文档摄入，再执行问答

#### Scenario: 复杂功能在后
- **WHEN** 所有基础测试通过
- **THEN** 再执行多轮对话、Graph 工作流等复杂测试

### Requirement: 测试覆盖率

测试 SHALL 覆盖所有核心功能模块。

#### Scenario: 模块覆盖
- **WHEN** 测试执行完成
- **THEN** 覆盖以下模块：Skills、RAG、Tools、Chat、Graph、Observability

#### Scenario: 端点覆盖
- **WHEN** 测试执行完成
- **THEN** 至少调用以下端点：`/api/skills`、`/api/rag/ingest`、`/api/rag/ask`、`/api/tools/chat`、`/api/graph/execute`、`/api/observability/health`

#### Scenario: 工具覆盖
- **WHEN** 测试执行完成
- **THEN** 至少触发以下工具：股价查询、市场指标、风险计算

### Requirement: 测试执行时间

测试 SHALL 在合理时间内完成。

#### Scenario: 正常执行时间
- **WHEN** 所有 AI API 可用
- **THEN** 测试总执行时间不超过 120 秒

#### Scenario: API 不可用时
- **WHEN** AI API 不可用导致测试跳过
- **THEN** 测试总执行时间不超过 30 秒
