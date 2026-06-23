## ADDED Requirements

### Requirement: 股价查询工具

系统 SHALL 注册股价查询工具，支持获取实时股票价格、历史数据和收益率计算。

#### Scenario: 查询股票实时价格
- **WHEN** Agent 调用 `getStockPrice` 工具并传入股票代码（如 "AAPL"）
- **THEN** 返回包含股票代码、名称、当前价格、涨跌幅、成交量、市值等信息

#### Scenario: 查询股票历史价格
- **WHEN** Agent 调用 `getStockHistory` 工具并传入股票代码和查询天数
- **THEN** 返回指定时间段内的价格数据，包括起始价、结束价、最高价、最低价、平均价

#### Scenario: 计算股票投资收益率
- **WHEN** Agent 调用 `calculateReturn` 工具并传入股票代码、买入价格、持股数量
- **THEN** 返回总成本、当前市值、收益金额、收益率等计算结果

#### Scenario: 无效股票代码处理
- **WHEN** Agent 调用 `getStockPrice` 工具并传入不存在的股票代码
- **THEN** 返回错误信息，提示股票代码不存在或暂不支持

### Requirement: 市场指标工具

系统 SHALL 注册市场指标工具，支持获取大盘指数、市场波动率、市场情绪和行业板块表现。

#### Scenario: 查询大盘指数
- **WHEN** Agent 调用 `getMarketIndex` 工具并传入指数名称（如 "上证指数"）
- **THEN** 返回指数代码、当前点位、涨跌幅、成交量等数据

#### Scenario: 查询市场波动率
- **WHEN** Agent 调用 `getMarketVolatility` 工具
- **THEN** 返回 VIX 指数值、风险等级（低/中等/偏高/高）和风险描述

#### Scenario: 查询市场情绪指标
- **WHEN** Agent 调用 `getMarketSentiment` 工具
- **THEN** 返回上涨家数、下跌家数、平盘家数、涨停数、跌停数、总成交量、市场情绪判断

#### Scenario: 查询行业板块表现
- **WHEN** Agent 调用 `getSectorPerformance` 工具并传入行业名称（如 "科技"）
- **THEN** 返回板块涨跌幅、领涨股列表、板块描述

#### Scenario: 无效指数/行业处理
- **WHEN** Agent 调用 `getMarketIndex` 或 `getSectorPerformance` 并传入不存在的名称
- **THEN** 返回错误信息，提示名称不存在或暂不支持

### Requirement: 风险计算工具

系统 SHALL 注册风险计算工具，支持投资组合收益计算、VaR 计算和夏普比率计算。

#### Scenario: 计算投资组合预期收益
- **WHEN** Agent 调用 `calculatePortfolioReturn` 工具并传入股票、债券、现金配置比例（总和必须为100%）
- **THEN** 返回预期年化收益率、风险等级和风险描述

#### Scenario: 计算 VaR（在险价值）
- **WHEN** Agent 调用 `calculateValueAtRisk` 工具并传入投资金额、置信水平、持有天数
- **THEN** 返回 VaR 值、VaR 百分比和风险描述

#### Scenario: 计算夏普比率
- **WHEN** Agent 调用 `calculateSharpeRatio` 工具并传入组合预期收益率、无风险利率、组合波动率
- **THEN** 返回夏普比率值和评价（优秀/良好/一般/较差）

#### Scenario: 配置比例验证
- **WHEN** Agent 调用 `calculatePortfolioReturn` 工具并传入的配置比例总和不等于100%
- **THEN** 返回错误信息，提示配置比例之和必须为100%

### Requirement: 投资工具系统提示词

系统 SHALL 为 ToolChatService 配置投资工具使用指南的系统提示词。

#### Scenario: 工具选择指导
- **WHEN** 用户询问股票、市场或风险相关问题
- **THEN** 系统提示词指导模型选择合适的投资工具

#### Scenario: 工具使用说明
- **WHEN** 模型决定调用投资工具
- **THEN** 系统提示词说明每个工具的用途和使用场景

#### Scenario: 风险声明
- **WHEN** 模型生成投资相关回答
- **THEN** 系统提示词要求声明投资风险，建议仅供参考

### Requirement: 投资工具注册

系统 SHALL 将所有投资工具注册到 ToolCallbackProvider 中，供 ReactAgent 和 ToolChatService 使用。

#### Scenario: 工具自动注册
- **WHEN** 应用启动时
- **THEN** 所有 `@Tool` 注解的方法自动注册到 MethodToolCallbackProvider

#### Scenario: 工具可用性验证
- **WHEN** ReactAgent 或 ToolChatService 调用工具
- **THEN** 所有投资工具均可正常调用并返回数据

### Requirement: 投资工具数据模拟

系统 SHALL 为投资工具提供模拟数据，支持离线测试和演示。

#### Scenario: 股票数据模拟
- **WHEN** 调用股价查询工具
- **THEN** 返回预设的模拟股票数据（AAPL、MSFT、GOOGL 等）

#### Scenario: 市场数据模拟
- **WHEN** 调用市场指标工具
- **THEN** 返回预设的模拟市场数据（上证指数、深成指、创业板指等）

#### Scenario: 风险计算模拟
- **WHEN** 调用风险计算工具
- **THEN** 基于预设参数计算并返回风险指标
