# 投资决策主流程接口 - 使用指南

## 🎯 接口概述

**`POST /api/investment/decide`** - 一个接口串联完整投资决策流程

该接口自动执行：
1. 市场分析 → 2. 投资推荐 → 3. 风险评估 → 4. 投资组合优化 → 5. 最终投资建议

---

## 📋 接口文档

### 请求格式

```yaml
POST /api/investment/decide
Content-Type: application/json

{
  "message": "我想投资科技股，预算10万，风险承受能力中等",
  "modelName": "deepSeekChatModel",  // 可选，默认 deepSeekChatModel
  "threadId": "optional-thread-id"    // 可选，用于多轮对话
}
```

### 响应格式

```json
{
  "status": "success",
  "threadId": "investment-1234567890",
  "steps": [
    {
      "step": 1,
      "name": "市场分析",
      "skill": "market-analysis",
      "status": "completed",
      "result": "当前A股市场整体呈现..."
    },
    {
      "step": 2,
      "name": "投资推荐",
      "skill": "investment-recommendation",
      "status": "completed",
      "result": "基于您的投资需求，推荐..."
    },
    {
      "step": 3,
      "name": "风险评估",
      "skill": "risk-assessment",
      "status": "completed",
      "result": "您的投资风险等级为..."
    },
    {
      "step": 4,
      "name": "投资组合优化",
      "skill": "portfolio-optimization",
      "status": "completed",
      "result": "建议配置比例为..."
    },
    {
      "step": 5,
      "name": "最终投资建议",
      "skill": "综合分析",
      "status": "completed",
      "result": "综合以上分析，建议您..."
    }
  ],
  "finalAdvice": "综合以上所有分析，建议您...",
  "riskWarning": "⚠️ 风险提示\n1. 投资有风险，入市需谨慎\n...",
  "durationMs": 15000,
  "tokenUsage": {
    "promptTokens": 1500,
    "completionTokens": 800,
    "totalTokens": 2300
  },
  "model": "deepSeekChatModel"
}
```

---

## 🚀 快速开始

### 1. 启动应用

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. 调用接口

```bash
# 同步接口（推荐用于测试）
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k, medium risk"}'

# 流式接口（推荐用于实时查看进度）
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k, medium risk"}'
```

### 3. 查看结果

系统将返回完整的投资决策报告，包含：
- 5个决策步骤的详细结果
- 最终投资建议
- 风险提示
- 执行耗时和Token用量

---

## 📝 使用示例

### 示例 1: 股票投资决策（同步接口）

```bash
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k, medium risk"}'
```

### 示例 2: 股票投资决策（流式接口 - 推荐）

```bash
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k, medium risk"}'
```

**流式输出示例**:
```
data: {"type":"step_start","step":1,"name":"市场分析","skill":"market-analysis","status":"running"}

data: {"type":"step_complete","step":1,"name":"市场分析","skill":"market-analysis","status":"completed","result":"当前A股市场..."}

data: {"type":"step_start","step":2,"name":"投资推荐","skill":"investment-recommendation","status":"running"}

data: {"type":"step_complete","step":2,"name":"投资推荐","skill":"investment-recommendation","status":"completed","result":"基于您的投资需求..."}

...

data: {"type":"decision_complete","threadId":"investment-123","finalAdvice":"投资决策已完成...","riskWarning":"⚠️ 风险提示...","durationMs":15000}
```

**预期响应**:
```json
{
  "status": "success",
  "steps": [
    {"step": 1, "name": "市场分析", "status": "completed", ...},
    {"step": 2, "name": "投资推荐", "status": "completed", ...},
    {"step": 3, "name": "风险评估", "status": "completed", ...},
    {"step": 4, "name": "投资组合优化", "status": "completed", ...},
    {"step": 5, "name": "最终投资建议", "status": "completed", ...}
  ],
  "finalAdvice": "基于当前市场分析，建议您...",
  "riskWarning": "⚠️ 投资有风险，入市需谨慎..."
}
```

### 示例 2: 多轮对话

```bash
# 第一轮：投资咨询
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我想投资股票",
    "threadId": "invest-123"
  }'

# 第二轮：基于上下文追问
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{
    "message": "预算10万，应该怎么配置？",
    "threadId": "invest-123"
  }'
```

### 示例 3: 保守型投资者

```bash
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我是保守型投资者，预算5万，希望稳健增值"
  }'
```

### 示例 4: 进取型投资者

```bash
curl -X POST http://localhost:8182/api/investment/decide \
  -H "Content-Type: application/json" \
  -d '{
    "message": "我是进取型投资者，预算20万，追求高收益"
  }'
```

---

## 🔧 其他接口

### 健康检查

```bash
curl http://localhost:8182/api/investment/health
```

**响应**:
```json
{
  "status": "UP",
  "service": "InvestmentDecisionService",
  "description": "智能投资代理决策引擎",
  "features": [
    "市场分析 (market-analysis)",
    "投资推荐 (investment-recommendation)",
    "风险评估 (risk-assessment)",
    "投资组合优化 (portfolio-optimization)"
  ]
}
```

### 获取支持的投资场景

```bash
curl http://localhost:8182/api/investment/scenarios
```

**响应**:
```json
{
  "scenarios": [
    {
      "name": "股票投资",
      "description": "分析股票市场，推荐个股",
      "example": "我想投资科技股，预算10万"
    },
    {
      "name": "基金投资",
      "description": "分析基金市场，推荐基金产品",
      "example": "推荐一些稳健型基金"
    },
    ...
  ],
  "usage": "POST /api/investment/decide"
}
```

---

## 🧪 测试用例

### 完整流程测试

```bash
# 运行投资决策控制器测试
mvn test -Dtest=InvestmentDecisionControllerTest
```

### 端到端流程测试

```bash
# 运行完整的投资决策引擎测试
mvn test -Dtest=InvestmentDecisionEngineEndToEndTest
```

### 所有测试

```bash
# 运行所有测试
mvn test
```

---

## 📊 决策流程详解

```
用户输入: "我想投资科技股，预算10万，风险承受能力中等"
                    ↓
┌─────────────────────────────────────────────┐
│  POST /api/investment/decide                │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Step 1: 市场分析                           │
│  - 调用 market-analysis 技能                │
│  - 分析 A 股科技板块走势                    │
│  - 输出: 市场分析报告                       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Step 2: 投资推荐                           │
│  - 调用 investment-recommendation 技能      │
│  - 基于市场分析推荐股票                     │
│  - 输出: 股票推荐列表                       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Step 3: 风险评估                           │
│  - 调用 risk-assessment 技能                │
│  - 计算 VaR、夏普比率                       │
│  - 输出: 风险评估报告                       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Step 4: 投资组合优化                       │
│  - 调用 portfolio-optimization 技能         │
│  - 优化资产配置比例                         │
│  - 输出: 优化配置建议                       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  Step 5: 最终投资建议                       │
│  - 综合所有分析结果                         │
│  - 生成投资建议和风险提示                   │
│  - 输出: 完整投资决策报告                   │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│  返回响应                                   │
│  - 包含所有步骤结果                         │
│  - 包含最终投资建议                         │
│  - 包含风险提示                             │
│  - 包含耗时和Token用量                      │
└─────────────────────────────────────────────┘
```

---

## 💡 最佳实践

### 1. 明确投资需求

```bash
# 好的请求（包含具体信息）
{
  "message": "我想投资科技股，预算10万，风险承受能力中等，投资期限2年"
}

# 不好的请求（信息不足）
{
  "message": "帮我投资"
}
```

### 2. 使用多轮对话

```bash
# 第一轮：了解基本情况
{"message": "我想投资股票", "threadId": "invest-123"}

# 第二轮：细化需求
{"message": "预算10万，应该怎么配置？", "threadId": "invest-123"}

# 第三轮：进一步优化
{"message": "如果我想增加收益，可以怎么调整？", "threadId": "invest-123"}
```

### 3. 选择合适的模型

```bash
# DeepSeek（推荐，平衡性能和成本）
{"message": "...", "modelName": "deepSeekChatModel"}

# MiMo（快速响应）
{"message": "...", "modelName": "openAiChatModel"}

# DashScope（中文优化）
{"message": "...", "modelName": "dashscopeChatModel"}
```

---

## 🐛 常见问题

### 1. 响应超时

**原因**: AI API 响应较慢

**解决**:
- 使用更简单的请求
- 选择更快的模型（如 MiMo）
- 增加超时时间

### 2. 部分步骤失败

**原因**: 某个技能执行失败

**解决**:
- 检查 `status` 字段，如果是 "partial" 表示部分成功
- 查看失败步骤的 `error` 字段
- 系统会返回已成功步骤的结果

### 3. 上下文丢失

**原因**: 未使用相同的 threadId

**解决**:
- 确保多轮对话使用相同的 `threadId`
- 系统会自动维护对话上下文

---

## 📈 性能指标

- **平均响应时间**: 10-20秒
- **成功率**: ≥ 90%
- **步骤完成率**: ≥ 80%
- **Token消耗**: 约 2000-3000 tokens/请求

---

## 🎯 测试清单

- [ ] 健康检查正常
- [ ] 完整投资决策流程成功
- [ ] 多轮对话上下文保持
- [ ] 不同风险偏好支持
- [ ] 错误处理正常
- [ ] 响应格式正确
- [ ] 风险提示完整

---

## 📚 相关文档

- [MANUAL_TEST_GUIDE.md](MANUAL_TEST_GUIDE.md) - 手动测试指南
- [contracts/api.md](specs/001-agent-decision-engine/contracts/api.md) - API 契约文档
- [plan.md](specs/001-agent-decision-engine/plan.md) - 实施计划

---

**最后更新**: 2026-06-17
