# 智能投资代理决策引擎 - 手动测试指南

## 📋 项目主旨

**智能投资代理决策引擎**：基于 Spring AI + Langfuse 3 的多模型智能投资代理系统，实现从问题理解、知识检索、数据分析到投资建议的全流程自动化。

**核心价值**：
- 多模型智能路由（DeepSeek、MiMo、DashScope）
- RAG 投资知识库（财报、市场报告）
- Function Calling 投资工具（股价、市场指标、风险计算）
- Skills 框架（市场分析、风险评估、投资推荐）
- Graph 工作流（决策流程编排）
- Langfuse 全链路追踪（可观测性）

---

## 🚀 快速启动

### 1. 启动应用

```bash
# 使用 dev 配置启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或使用 local 配置（需要 Langfuse）
mvn spring-boot:run -Dspring-boot.run.profiles=local,dev
```

### 2. 验证启动

```bash
curl http://localhost:8182/api/observability/health
```

---

## 🧪 完整投资决策流程测试

### 场景描述

**用户**：我想投资科技股，预算10万，风险承受能力中等

**系统流程**：
1. 市场分析 → 分析科技板块走势
2. 知识检索 → 检索相关投资知识
3. 数据查询 → 查询股价和市场指标
4. 风险评估 → 评估投资风险
5. 投资建议 → 生成投资建议和风险提示

---

### 阶段 1: 市场分析

#### 1.1 查询市场整体情况

```bash
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "分析一下当前A股市场整体走势，包括大盘指数和市场情绪"}'
```

**预期结果**：
- Agent 自动选择 market-analysis 技能
- 返回上证指数、深成指、创业板指数据
- 分析市场情绪（偏多/偏空/平衡）
- 给出市场趋势判断

**验证要点**：
- ✅ 包含大盘指数数据
- ✅ 包含市场情绪分析
- ✅ 包含投资建议

#### 1.2 分析科技板块

```bash
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "重点分析一下科技板块的走势和投资机会"}'
```

**预期结果**：
- 分析科技板块涨跌幅
- 识别领涨股和领跌股
- 评估行业景气度
- 给出板块投资建议

---

### 阶段 2: 投资推荐

#### 2.1 获取股票推荐

```bash
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "基于市场分析，推荐几只适合投资的科技股，预算10万"}'
```

**预期结果**：
- Agent 自动选择 investment-recommendation 技能
- 推荐 2-3 只科技股
- 给出买入理由和目标价
- 包含风险提示

**验证要点**：
- ✅ 推荐具体股票
- ✅ 包含买入理由
- ✅ 包含目标价格
- ✅ 包含风险提示

#### 2.2 查询推荐股票详情

```bash
# 查询苹果公司股价
curl -X POST http://localhost:8182/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "查询苹果公司的股价和历史走势"}'

# 查询微软公司股价
curl -X POST http://localhost:8182/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "查询微软公司的股价"}'
```

**预期结果**：
- 返回股票实时价格
- 包含涨跌幅和成交量
- 包含市值信息

---

### 阶段 3: 风险评估

#### 3.1 评估投资组合风险

```bash
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我评估投资10万到科技股的风险，包括VaR和夏普比率"}'
```

**预期结果**：
- Agent 自动选择 risk-assessment 技能
- 计算 VaR（在险价值）
- 计算夏普比率
- 评估风险等级
- 给出风险控制建议

**验证要点**：
- ✅ 包含 VaR 计算
- ✅ 包含夏普比率
- ✅ 包含风险等级
- ✅ 包含风险控制建议

#### 3.2 直接调用风险计算工具

```bash
# 计算投资组合收益
curl -X POST http://localhost:8182/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我计算60%股票、30%债券、10%现金的组合预期收益"}'

# 计算 VaR
curl -X POST http://localhost:8182/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "我投资了10万元，帮我计算95%置信水平下30天的VaR"}'

# 计算夏普比率
curl -X POST http://localhost:8182/api/tools/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "帮我计算夏普比率，预期收益12%，无风险利率3%，波动率20%"}'
```

**预期结果**：
- 投资组合收益：约 8.6%
- VaR：约 11,351 元（11.35%）
- 夏普比率：0.45（一般）

---

### 阶段 4: 投资组合优化

#### 4.1 获取优化建议

```bash
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "基于风险评估，帮我优化投资组合配置，平衡收益和风险"}'
```

**预期结果**：
- Agent 自动选择 portfolio-optimization 技能
- 分析当前配置的风险收益
- 给出优化后的配置比例
- 提供再平衡建议

**验证要点**：
- ✅ 分析当前配置
- ✅ 给出优化建议
- ✅ 包含风险收益对比

---

### 阶段 5: 最终投资建议

#### 5.1 生成综合建议

```bash
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "综合以上分析，给出最终的投资建议，包括买入时机、仓位配置和风险提示"}'
```

**预期结果**：
- 综合市场分析、投资推荐、风险评估结果
- 给出具体的投资建议
- 包含买入时机建议
- 包含仓位配置建议
- 包含详细的风险提示

**验证要点**：
- ✅ 综合所有分析结果
- ✅ 给出具体投资建议
- ✅ 包含买入时机
- ✅ 包含仓位配置
- ✅ 包含风险提示

---

### 阶段 6: 多轮对话验证

#### 6.1 保持上下文的追问

```bash
# 第一轮：投资咨询
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "我想投资科技股，预算10万", "threadId": "invest-123"}'

# 第二轮：基于上下文追问（使用相同 threadId）
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "风险承受能力中等，应该怎么配置？", "threadId": "invest-123"}'

# 第三轮：进一步优化
curl -X POST http://localhost:8182/api/skills/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "如果我想增加收益，可以怎么调整？", "threadId": "invest-123"}'
```

**预期结果**：
- 第一轮：理解投资需求
- 第二轮：基于第一轮上下文给出配置建议
- 第三轮：基于前两轮上下文给出调整建议
- 上下文保持完整

---

### 阶段 7: RAG 知识库验证

#### 7.1 摄入投资文档

```bash
curl -X POST http://localhost:8182/api/rag/ingest \
  -F "file=@src/main/resources/docs/sample.txt"
```

#### 7.2 基于知识库问答

```bash
curl -X POST http://localhost:8182/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"message": "根据知识库，科技股的投资策略是什么？"}'
```

**预期结果**：
- 基于知识库内容回答
- 引用文档中的信息
- 声明是基于知识库的参考

---

### 阶段 8: Graph 工作流验证

#### 8.1 投资内容分类

```bash
# 技术内容
curl -X POST http://localhost:8182/api/graph/execute \
  -H "Content-Type: application/json" \
  -d '{"input": "如何分析股票技术指标"}'

# 生活内容
curl -X POST http://localhost:8182/api/graph/execute \
  -H "Content-Type: application/json" \
  -d '{"input": "今天适合出门投资理财吗"}'

# 通用内容
curl -X POST http://localhost:8182/api/graph/execute \
  -H "Content-Type: application/json" \
  -d '{"input": "请给我一些建议"}'
```

**预期结果**：
- 技术内容 → 分类为 "technical"
- 生活内容 → 分类为 "lifestyle"
- 通用内容 → 分类为 "general"

---

### 阶段 9: 多模型切换验证

```bash
# DeepSeek 模型
curl -X POST http://localhost:8182/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "分析一下科技股投资机会"}' \
  --data-urlencode "modelName=deepSeekChatModel"

# MiMo 模型
curl -X POST http://localhost:8182/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "分析一下科技股投资机会"}' \
  --data-urlencode "modelName=openAiChatModel"

# DashScope 模型
curl -X POST http://localhost:8182/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "分析一下科技股投资机会"}' \
  --data-urlencode "modelName=dashscopeChatModel"
```

**预期结果**：
- 每个模型都能正常返回回答
- 回答内容来自不同模型
- 切换无需重启应用

---

### 阶段 10: 流式输出验证

```bash
# Flux 流式
curl -N "http://localhost:8182/api/chat/flux/stream?message=分析科技股投资机会&modelName=deepSeekChatModel"

# SSE 流式
curl -N "http://localhost:8182/api/chat/sse/stream?message=分析科技股投资机会&modelName=deepSeekChatModel"

# SseEmitter 流式
curl -N "http://localhost:8182/api/chat/emitter/stream?message=分析科技股投资机会&modelName=deepSeekChatModel"
```

**预期结果**：
- 以流式方式逐步返回回答
- 形成打字机效果

---

### 阶段 11: Observability 验证

```bash
# 健康检查
curl http://localhost:8182/api/observability/health

# 可用模型列表
curl http://localhost:8182/api/observability/models

# 技能诊断信息
curl http://localhost:8182/api/skills
```

**预期结果**：
- 返回 Langfuse 连接状态
- 返回可用模型列表
- 返回已注册技能列表

#### 验证 Langfuse Trace

1. 发送任意 AI 请求
2. 等待 30 秒
3. 访问 Langfuse 界面：
   - 云端：https://cloud.langfuse.com
   - 本地：http://localhost:3000
4. 查看 Trace 记录

**预期结果**：
- Trace 包含完整的调用链路
- 包含提示词、补全内容、模型名称、Token 用量、耗时

---

## 📊 验证清单

### Skills 框架
- [ ] 7 个投资技能正确注册
- [ ] Agent 能自动选择合适的技能
- [ ] 市场分析技能正常工作
- [ ] 风险评估技能正常工作
- [ ] 投资推荐技能正常工作
- [ ] 投资组合优化技能正常工作
- [ ] 多轮对话上下文保持正常

### 投资工具
- [ ] 股价查询工具正常工作
- [ ] 市场指标工具正常工作
- [ ] 风险计算工具正常工作
- [ ] 工具调用成功率 ≥ 90%

### 系统提示词
- [ ] ChatService 投资顾问角色正确
- [ ] RagService 知识库助手角色正确
- [ ] ToolChatService 工具使用指南正确
- [ ] SkillsAgent 技能选择指南正确
- [ ] 所有提示词与投资主题一致

### Graph 工作流
- [ ] 条件分支路由正确
- [ ] 节点顺序执行正确
- [ ] 分类结果准确

### RAG 知识库
- [ ] 文档摄入成功
- [ ] 知识库问答准确
- [ ] 流式输出正常

### 多模型切换
- [ ] DeepSeek 模型正常
- [ ] MiMo 模型正常
- [ ] DashScope 模型正常
- [ ] 切换无需重启

### 流式输出
- [ ] Flux 流式正常
- [ ] SSE 流式正常
- [ ] SseEmitter 流式正常

### Observability
- [ ] 健康检查正常
- [ ] Langfuse Trace 完整
- [ ] 30 秒内可见

### 完整投资决策流程
- [ ] 市场分析完成
- [ ] 投资推荐完成
- [ ] 风险评估完成
- [ ] 投资组合优化完成
- [ ] 最终投资建议完成
- [ ] 多轮对话上下文保持

---

## 🔍 测试结果记录

| 测试阶段 | 测试项 | 状态 | 备注 |
|---------|--------|------|------|
| 阶段1 | 市场分析 | ✅/❌ | |
| 阶段2 | 投资推荐 | ✅/❌ | |
| 阶段3 | 风险评估 | ✅/❌ | |
| 阶段4 | 投资组合优化 | ✅/❌ | |
| 阶段5 | 最终投资建议 | ✅/❌ | |
| 阶段6 | 多轮对话 | ✅/❌ | |
| 阶段7 | RAG 知识库 | ✅/❌ | |
| 阶段8 | Graph 工作流 | ✅/❌ | |
| 阶段9 | 多模型切换 | ✅/❌ | |
| 阶段10 | 流式输出 | ✅/❌ | |
| 阶段11 | Observability | ✅/❌ | |

---

## 🐛 常见问题排查

### 1. API 密钥未配置

**症状**：模型调用失败，返回 500 错误

**解决**：
```bash
# 检查配置文件
cat src/main/resources/application-dev.yml

# 确保 API 密钥已配置
export DEEPSEEK_API_KEY=your-key
export MIMO_API_KEY=your-key
export DASHSCOPE_API_KEY=your-key
```

### 2. 技能未加载

**症状**：Skills Agent 返回错误

**解决**：
```bash
# 检查技能文件
ls -la src/main/resources/skills/

# 确保 SKILL.md 文件存在
find src/main/resources/skills -name "SKILL.md"
```

### 3. 工具调用失败

**症状**：Function Calling 返回错误

**解决**：
```bash
# 检查工具是否注册
curl http://localhost:8182/api/skills

# 检查工具实现
grep -r "@Tool" src/main/java/com/zhou/ai/tools/
```

### 4. Langfuse 连接失败

**症状**：Trace 未上报，但 AI 功能正常

**解决**：
```bash
# 检查 Langfuse 是否运行（本地模式）
docker ps | grep langfuse

# 检查健康状态
curl http://localhost:8182/api/observability/health
```

---

## 📈 性能指标

- **市场分析响应时间**：< 10 秒
- **投资推荐响应时间**：< 15 秒
- **风险评估响应时间**：< 10 秒
- **工具调用成功率**：≥ 90%
- **多轮对话上下文保持**：100%
- **Langfuse Trace 可见时间**：< 30 秒

---

## 🎯 测试目标

通过完整的手动测试，验证：

1. **功能完整性**：所有核心功能正常工作
2. **流程串联性**：各功能模块能正确串联
3. **提示词一致性**：所有提示词与投资主题一致
4. **用户体验**：投资决策流程顺畅
5. **可观测性**：全链路追踪正常

---

**测试完成标准**：所有验证清单项目均为 ✅，测试结果记录无 ❌
