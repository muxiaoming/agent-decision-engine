## ADDED Requirements

### Requirement: Tool function registration

系统 SHALL 使用 `@Tool` 注解注册自定义工具函数，支持在 LLM 聊天补全过程中被调用。

#### Scenario: Register weather tool
- **WHEN** 一个 `@Component` 类中的方法使用 `@Tool` 注解标记并包含 `@ToolParam` 参数
- **THEN** 该工具被自动注册并可用于 LLM 函数调用

#### Scenario: Register multiple tools
- **WHEN** 多个工具函数被定义（如天气、计算器、股价查询、市场指标、风险计算）
- **THEN** 所有工具通过 `MethodToolCallbackProvider` 自动注册，LLM 可根据用户意图选择合适的工具

#### Scenario: Tool auto-discovery
- **WHEN** 应用启动时
- **THEN** `ToolCallbackProvider` 自动扫描所有 `@Tool` 注解的方法并注册为 `ToolCallback`

### Requirement: Function calling REST endpoint

系统 SHALL 暴露 `POST /api/tools/chat` 端点，发送用户消息到 LLM 并启用已注册的工具，允许模型调用工具并返回最终答案。

#### Scenario: LLM invokes tool
- **WHEN** 客户端发送 `POST /api/tools/chat` with `{"message": "北京今天天气怎么样？"}` and optional `?modelName=deepSeekChatModel`
- **THEN** LLM 调用天气工具获取天气数据，接收结果，并返回包含工具结果的自然语言回答

#### Scenario: No tool invocation needed
- **WHEN** 客户端发送 `POST /api/tools/chat` with `{"message": "给我讲个笑话"}` and optional `?modelName=deepSeekChatModel`
- **THEN** LLM 直接响应而不调用任何工具

#### Scenario: Investment tool invocation
- **WHEN** 客户端发送 `POST /api/tools/chat` with `{"message": "查询苹果公司的股价"}` and optional `?modelName=deepSeekChatModel`
- **THEN** LLM 调用股价查询工具获取实时数据，返回包含股价信息的回答

#### Scenario: Risk calculation tool invocation
- **WHEN** 客户端发送 `POST /api/tools/chat` with `{"message": "帮我计算一下60%股票、30%债券、10%现金的组合收益"}` and optional `?modelName=deepSeekChatModel`
- **THEN** LLM 调用投资组合计算工具，返回预期收益和风险等级

### Requirement: Tool call tracing

系统 SHALL 将每次工具调用（函数名、输入参数、输出结果）记录为 OpenTelemetry trace span 的一部分。

#### Scenario: Tool call visible in Langfuse
- **WHEN** 聊天请求过程中执行了函数调用
- **THEN** Langfuse 追踪显示工具调用作为子 span，包含函数名、输入和输出

### Requirement: Tool system prompt

系统 SHALL 为 ToolChatService 配置系统提示词，指导 LLM 何时以及如何使用投资工具。

#### Scenario: Tool usage guidance
- **WHEN** 用户询问股票、市场或风险相关问题
- **THEN** 系统提示词指导模型选择合适的投资工具

#### Scenario: Investment disclaimer
- **WHEN** 模型生成投资相关回答
- **THEN** 系统提示词要求声明投资风险，建议仅供参考

### Requirement: ToolChatService integration

系统 SHALL 通过 ToolChatService 提供工具调用对话服务，自动装配所有注册的工具。

#### Scenario: Tool auto-assembly
- **WHEN** ToolChatService 创建 ChatClient
- **THEN** 通过 ModelRouter 自动装配所有 ToolCallback，无需硬编码工具名

#### Scenario: Dynamic model routing
- **WHEN** 客户端指定 modelName 参数
- **THEN** 请求路由到对应的模型，同时保持工具可用性
