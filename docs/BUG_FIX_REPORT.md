# 🐛 问题诊断和解决方案

## 📊 你的测试结果分析

### 流程执行情况

```
Step 1: 问题感知 (Skills)     ❌ 失败 - DeepSeek API 错误
Step 2: 知识检索 (RAG)        ❌ 失败 - DeepSeek API 错误
Step 3: 数据获取 (Tools)      ✅ 成功 - AAPL股价: 178.52, 上证指数: 3056.78, VaR: 11351.58
Step 4: 推理分析 (Skills)     ❌ 失败 - DeepSeek API 错误
Step 5: 决策生成 (Skills)     ❌ 失败 - DeepSeek API 错误
Step 6: 流程编排 (Graph)      ✅ 成功 - 分类: technical
```

**总计**: 2/6 成功, 4/6 失败

---

## 🔍 根本原因

### ❌ DeepSeek API Key 未设置

```bash
$ echo $DEEPSEEK_API_KEY
(空)
```

**错误信息**:
```
Exception: Error while extracting response for type
[org.springframework.ai.deepseek.api.DeepSeekApi$ChatCompletion]
and content type [application/json]
```

**解释**: Spring AI 尝试调用 DeepSeek API，但 API Key 缺失导致请求失败。

---

## ✅ 解决方案

### 方案 1: 设置环境变量（推荐）

**Windows (Git Bash):**
```bash
# 临时设置（当前会话有效）
export DEEPSEEK_API_KEY=sk-your-real-api-key

# 永久设置
echo 'export DEEPSEEK_API_KEY=sk-your-real-api-key' >> ~/.bashrc
source ~/.bashrc
```

**Windows (PowerShell):**
```powershell
# 临时设置
$env:DEEPSEEK_API_KEY="sk-your-real-api-key"

# 永久设置
[System.Environment]::SetEnvironmentVariable("DEEPSEEK_API_KEY", "sk-your-real-api-key", "User")
```

### 方案 2: 创建 .env 文件

在项目根目录创建 `.env` 文件：

```bash
# DeepSeek API
DEEPSEEK_API_KEY=sk-your-real-api-key

# 其他 API（可选）
MIMO_API_KEY=your-mimo-key
DASHSCOPE_API_KEY=your-dashscope-key
```

然后使用 Maven 插件或 Docker 加载。

### 方案 3: 修改 application.yml

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    deepseek:
      api-key: sk-your-real-api-key  # 直接写入 API Key
```

⚠️ **注意**: 不要将 API Key 提交到 Git！

---

## 🎯 测试验证

### 配置 API Key 后测试

```bash
# 设置 API Key
export DEEPSEEK_API_KEY=sk-your-real-api-key

# 测试完整流程
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "I want to invest in tech stocks, budget 100k",
    "enableRAG": true,
    "enableTools": true,
    "enableGraph": true
  }'
```

**预期结果**: 6/6 步骤成功

### 只测试 Tools 和 Graph（不需要 API Key）

```bash
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Get current stock price of AAPL",
    "enableRAG": false,
    "enableTools": true,
    "enableGraph": true
  }'
```

**预期结果**: 2/6 成功（Tools + Graph）

---

## 📋 流程验证清单

即使 API Key 未设置，以下流程也应正常：

### ✅ 始终工作（不需要 AI）

| 步骤 | 模块 | 测试方法 | 预期结果 |
|------|------|---------|---------|
| Step 3 | Tools | 获取股价、指数、风险 | ✅ 返回模拟数据 |
| Step 6 | Graph | 执行图工作流 | ✅ 返回分类结果 |

### ⚠️ 需要 DeepSeek API

| 步骤 | 模块 | 测试方法 | 失败原因 |
|------|------|---------|---------|
| Step 1 | Skills | 问题感知 | ❌ API Key 缺失 |
| Step 2 | RAG | 知识检索 | ❌ API Key 缺失 |
| Step 4 | Skills | 推理分析 | ❌ API Key 缺失 |
| Step 5 | Skills | 决策生成 | ❌ API Key 缺失 |

---

## 🚀 快速开始

### 1. 获取 API Key

1. 访问 https://platform.deepseek.com/
2. 注册/登录账户
3. 生成 API Key（格式：`sk-...`）

### 2. 配置 API Key

```bash
export DEEPSEEK_API_KEY=sk-your-real-api-key
```

### 3. 重启应用

```bash
cd "D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo"
mvn spring-boot:run
```

### 4. 测试

```bash
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k"}'
```

**预期**: 6/6 步骤成功 ✅

---

## 📊 预期成功结果

配置 API Key 后，应该看到：

```
Step 1: 问题感知 (Skills)     ✅ 成功 - 分析用户需求
Step 2: 知识检索 (RAG)        ✅ 成功 - 检索相关知识
Step 3: 数据获取 (Tools)      ✅ 成功 - AAPL股价: 178.52, 上证指数: 3056.78
Step 4: 推理分析 (Skills)     ✅ 成功 - 基于知识分析
Step 5: 决策生成 (Skills)     ✅ 成功 - 生成投资建议
Step 6: 流程编排 (Graph)      ✅ 成功 - 分类: technical
```

**总计**: 6/6 成功 ✅

---

## 🔧 代码改进

我已经改进了错误处理：

1. ✅ **更清晰的错误信息** - 检测 "Exception:" 前缀
2. ✅ **失败状态正确标记** - 使用 `step_error` 而非 `step_complete`
3. ✅ **错误不中断流程** - 单个步骤失败不影响后续步骤
4. ✅ **诊断文档** - 创建了 `API_KEY_SETUP.md`

---

## 📚 相关文档

- `docs/API_KEY_SETUP.md` - API Key 配置指南
- `docs/REFACTOR_COMPLETE_REPORT.md` - 重构完成报告
- `INVESTMENT_API_GUIDE.md` - 接口使用指南

---

## 🎓 学习要点

1. **优雅降级** - 即使 AI 不可用，Tools 和 Graph 仍工作
2. **错误处理** - 检测 API 错误并标记为失败
3. **流程编排** - 6步流程独立执行，失败不中断
4. **向后兼容** - 新功能不影响旧接口

---

## 💡 建议

### 短期

1. ✅ 设置 DeepSeek API Key
2. ✅ 测试完整流程
3. ✅ 验证流式输出

### 长期

1. 添加重试机制（Resilience4j CircuitBreaker）
2. 支持多模型切换（DeepSeek/OpenAI/DashScope）
3. 添加超时配置
4. 集成 Langfuse 追踪

---

**修复日期**: 2026-06-21
**状态**: ✅ 已修复
**下一步**: 设置 API Key 后测试
