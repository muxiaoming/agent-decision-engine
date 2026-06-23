## ADDED Requirements

### Requirement: ChatService 系统提示词

系统 SHALL 为 ChatService 配置投资顾问角色的系统提示词，定义 AI 助手的核心行为。

#### Scenario: 投资顾问角色定义
- **WHEN** ChatService 处理同步或流式对话请求
- **THEN** 系统提示词将 AI 助手定位为"智能投资顾问助手"，专注于提供专业的投资建议和财务分析

#### Scenario: 行为指南
- **WHEN** AI 助手生成回答
- **THEN** 系统提示词要求：理解用户投资需求和风险偏好、基于真实数据和专业知识给出建议、说明投资风险避免过度承诺、用通俗易懂的语言解释复杂金融概念、不确定时说明需要进一步了解

#### Scenario: 流式对话支持
- **WHEN** ChatService 处理流式对话请求
- **THEN** 系统提示词在同步和流式模式下保持一致

### Requirement: RagService 系统提示词

系统 SHALL 为 RagService 配置投资知识库助手角色的系统提示词，定义基于文档回答的行为。

#### Scenario: 知识库助手角色定义
- **WHEN** RagService 处理 RAG 问答请求
- **THEN** 系统提示词将 AI 助手定位为"投资知识库助手"，专门基于投资相关文档和资料回答问题

#### Scenario: 知识引用行为
- **WHEN** 知识库中有相关信息
- **THEN** 系统提示词要求优先使用检索到的信息，准确引用并说明来源

#### Scenario: 知识缺失处理
- **WHEN** 知识库中没有相关信息
- **THEN** 系统提示词要求说明无法从知识库中找到，但仍可提供一般性建议

#### Scenario: 投资风险声明
- **WHEN** 基于财务数据和投资建议回答
- **THEN** 系统提示词要求声明这是基于知识库的参考信息，不构成投资建议

#### Scenario: 专业态度
- **WHEN** 生成任何回答
- **THEN** 系统提示词要求保持专业、客观、谨慎的态度

### Requirement: ToolChatService 系统提示词

系统 SHALL 为 ToolChatService 配置投资工具使用指南的系统提示词，指导 LLM 何时以及如何使用工具。

#### Scenario: 工具分类说明
- **WHEN** 系统提示词被加载
- **THEN** 包含三大类工具说明：股价查询工具（getStockPrice、getStockHistory、calculateReturn）、市场指标工具（getMarketIndex、getMarketVolatility、getMarketSentiment、getSectorPerformance）、风险计算工具（calculatePortfolioReturn、calculateValueAtRisk、calculateSharpeRatio）

#### Scenario: 工具使用场景指导
- **WHEN** 用户询问股票相关问题
- **THEN** 系统提示词指导优先使用股价查询工具获取实时数据

#### Scenario: 市场数据获取指导
- **WHEN** 用户询问大盘或市场整体情况
- **THEN** 系统提示词指导使用市场指标工具

#### Scenario: 风险计算指导
- **WHEN** 用户需要计算收益、风险或优化配置
- **THEN** 系统提示词指导使用风险计算工具

#### Scenario: 工具调用原则
- **WHEN** Agent 决定调用工具
- **THEN** 系统提示词要求先理解用户需求，调用后给出专业分析和建议

#### Scenario: 投资风险声明
- **WHEN** 生成投资相关回答
- **THEN** 系统提示词要求始终声明投资有风险，建议仅供参考

### Requirement: SkillsAgent 系统提示词

系统 SHALL 为 SkillsAgent 配置技能选择和工具使用的系统提示词，指导 ReactAgent 的行为。

#### Scenario: 技能列表说明
- **WHEN** 系统提示词被加载
- **THEN** 包含所有可用技能的列表：market-analysis、risk-assessment、portfolio-optimization、investment-recommendation、weather-assistant、java-spring-expert、code-reviewer

#### Scenario: 技能选择指导
- **WHEN** 用户提出问题
- **THEN** 系统提示词指导 Agent 根据问题类型判断是否需要加载某个技能

#### Scenario: 技能加载机制
- **WHEN** Agent 决定需要某个技能
- **THEN** 系统提示词说明使用 `read_skill` 工具获取完整技能内容后再回答

#### Scenario: 工具协同使用
- **WHEN** Agent 加载技能后需要获取实时数据
- **THEN** 系统提示词说明可以使用 queryWeather、calculate 和投资相关工具来辅助回答

#### Scenario: 风险声明要求
- **WHEN** 生成任何投资相关回答
- **THEN** 系统提示词要求始终牢记投资有风险，建议仅供参考，不构成投资建议

### Requirement: 系统提示词一致性

系统 SHALL 确保所有服务的系统提示词在投资风险声明方面保持一致。

#### Scenario: 统一风险声明
- **WHEN** ChatService、RagService、ToolChatService、SkillsAgent 生成投资相关回答
- **THEN** 所有系统提示词都包含投资风险声明，建议仅供参考

#### Scenario: 角色定位一致
- **WHEN** 所有服务处理投资相关请求
- **THEN** 系统提示词都将 AI 助手定位为专业、客观、谨慎的投资顾问角色

### Requirement: 系统提示词可维护性

系统 SHALL 支持系统提示词的集中管理和更新。

#### Scenario: 提示词集中定义
- **WHEN** 开发者需要修改系统提示词
- **THEN** 每个服务的系统提示词作为常量定义在各自的 Service 类中

#### Scenario: 提示词版本控制
- **WHEN** 系统提示词发生变更
- **THEN** 变更通过 Git 版本控制进行追踪

#### Scenario: 提示词测试
- **WHEN** 修改系统提示词
- **THEN** 运行相关测试确保 AI 行为符合预期
