## ADDED Requirements

### Requirement: Skills 框架集成

系统 SHALL 集成 Spring AI Alibaba 的 Skills 框架，支持模块化的技能注册和动态加载。

#### Scenario: 技能自动注册
- **WHEN** 应用启动时 classpath 中存在符合规范的技能定义文件（`skills/*/SKILL.md`）
- **THEN** 系统自动发现并注册所有技能到 SkillRegistry，无需手动配置

#### Scenario: 渐进式披露机制
- **WHEN** ReactAgent 接收到用户查询时
- **THEN** 系统提示词仅包含技能名称和描述摘要，Agent 按需调用 `read_skill` 工具加载完整技能内容

#### Scenario: 技能列表查询
- **WHEN** 客户端发送 `GET /api/skills`
- **THEN** 返回所有已注册技能的名称和描述列表

#### Scenario: 技能对话端点
- **WHEN** 客户端发送 `POST /api/skills/chat` with `{"message": "分析一下A股市场", "threadId": "optional"}`
- **THEN** ReactAgent 根据用户意图选择合适的技能，加载技能内容，并生成专业回答

#### Scenario: 多轮对话上下文保持
- **WHEN** 客户端使用相同的 `threadId` 发送多次请求
- **THEN** 系统保持对话上下文，支持基于前文的追问和优化

### Requirement: 投资领域技能定义

系统 SHALL 注册以下投资相关技能：

#### Scenario: 市场分析技能
- **WHEN** 用户询问市场趋势、大盘走势、行业分析等问题
- **THEN** Agent 加载 `market-analysis` 技能，可调用市场指标工具获取实时数据

#### Scenario: 风险评估技能
- **WHEN** 用户询问投资风险、VaR 计算、风险控制等问题
- **THEN** Agent 加载 `risk-assessment` 技能，可调用风险计算工具进行量化分析

#### Scenario: 投资组合优化技能
- **WHEN** 用户询问资产配置、组合优化、再平衡等问题
- **THEN** Agent 加载 `portfolio-optimization` 技能，可调用投资组合计算工具

#### Scenario: 投资推荐技能
- **WHEN** 用户询问投资建议、买什么股票、投资策略等问题
- **THEN** Agent 加载 `investment-recommendation` 技能，可调用股价查询和市场指标工具

### Requirement: 技能工具集成

系统 SHALL 将投资工具集成到 ReactAgent 中，支持技能和工具的协同使用。

#### Scenario: 技能触发工具调用
- **WHEN** Agent 加载投资技能后需要获取实时数据
- **THEN** Agent 自动调用相应的投资工具（股价查询、市场指标、风险计算）

#### Scenario: 工具调用追踪
- **WHEN** 技能对话过程中调用工具
- **THEN** 每个工具调用作为子 Span 记录到 Langfuse 追踪中

### Requirement: 技能配置管理

系统 SHALL 通过配置类管理 Skills 框架的初始化和 ReactAgent 的构建。

#### Scenario: SkillRegistry Bean 配置
- **WHEN** 应用启动
- **THEN** 创建 ClasspathSkillRegistry Bean，配置 classpath 路径为 `skills`，启用自动加载

#### Scenario: SkillsAgentHook Bean 配置
- **WHEN** 应用启动
- **THEN** 创建 SkillsAgentHook Bean，关联 SkillRegistry，启用自动重载

#### Scenario: ReactAgent Bean 配置
- **WHEN** 应用启动
- **THEN** 创建 ReactAgent Bean，配置模型、系统提示词、工具集和 Hooks

### Requirement: 技能内容规范

系统 SHALL 要求技能定义文件符合以下规范：

#### Scenario: SKILL.md 文件格式
- **WHEN** 开发者创建新技能
- **THEN** 技能定义文件必须包含 YAML frontmatter（name、description）和 Markdown 内容

#### Scenario: 技能内容结构
- **WHEN** 技能定义文件被加载
- **THEN** 内容包含：技能说明、可用工具列表、回答流程、回答格式、示例

### Requirement: 系统提示词配置

系统 SHALL 为 ReactAgent 配置投资顾问角色的系统提示词。

#### Scenario: 技能选择指导
- **WHEN** ReactAgent 接收到用户查询
- **THEN** 系统提示词指导 Agent 根据问题类型选择合适的技能

#### Scenario: 工具使用指导
- **WHEN** Agent 需要获取实时数据
- **THEN** 系统提示词说明可用的工具和使用场景

#### Scenario: 风险声明
- **WHEN** Agent 生成投资相关回答
- **THEN** 系统提示词要求声明投资风险，建议仅供参考
