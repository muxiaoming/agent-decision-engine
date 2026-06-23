# 投资决策接口 - API Key 配置指南

## 问题诊断

如果看到 "Error while extracting response" 错误，通常是因为：

1. ❌ **API Key 未设置**
2. ❌ **API Key 无效**
3. ❌ **网络连接问题**

## 解决方案

### 方案 1：设置环境变量

**Windows (PowerShell):**
```powershell
$env:DEEPSEEK_API_KEY = "sk-your-real-api-key"
```

**Windows (Git Bash):**
```bash
export DEEPSEEK_API_KEY=sk-your-real-api-key
```

**Linux/macOS:**
```bash
export DEEPSEEK_API_KEY=sk-your-real-api-key
```

### 方案 2：创建 .env 文件

在项目根目录创建 `.env` 文件：
```bash
DEEPSEEK_API_KEY=sk-your-real-api-key
MIMO_API_KEY=your-mimo-key
DASHSCOPE_API_KEY=your-dashscope-key
```

然后使用 Docker 或 Maven 插件加载环境变量。

### 方案 3：在 application.yml 中配置

修改 `src/main/resources/application.yml`：
```yaml
spring:
  ai:
    deepseek:
      api-key: sk-your-real-api-key
```

## 验证配置

### 检查环境变量

```bash
# Windows
echo $DEEPSEEK_API_KEY

# Linux/macOS
echo $DEEPSEEK_API_KEY
```

### 测试 API 连接

```bash
curl -X POST https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 10
  }'
```

## 流程验证清单

即使 DeepSeek API 不可用，以下步骤应该工作：

✅ **Step 3: 数据获取 (Tools)** - 不需要 AI API
✅ **Step 6: 流程编排 (Graph)** - 不需要 AI API

以下步骤需要 DeepSeek API：

❌ **Step 1: 问题感知 (Skills)** - 需要 AI
❌ **Step 2: 知识检索 (RAG)** - 需要 AI
❌ **Step 4: 推理分析 (Skills)** - 需要 AI
❌ **Step 5: 决策生成 (Skills)** - 需要 AI

## 快速测试

如果不想配置 AI API，可以测试只启用 Tools 和 Graph：

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

这应该返回：
- Step 3 (Tools): ✅ 成功
- Step 6 (Graph): ✅ 成功
- Step 1, 4, 5 (Skills): ⚠️ 失败（但不中断）
- Step 2 (RAG): ⏭️ 跳过

## 常见错误

### 1. "Error while extracting response for type [ChatCompletion]"

**原因**: API 响应无法解析
**解决**: 检查 API Key 是否正确，网络是否通畅

### 2. "Invalid API Key"

**原因**: API Key 无效或已过期
**解决**: 生成新的 API Key

### 3. "Rate limit exceeded"

**原因**: 请求频率过高
**解决**: 等待后重试，或升级 API 配额

## 调试模式

启用详细日志：

```bash
export LOGGING_LEVEL_COM_ZHOU_AI=DEBUG
mvn spring-boot:run
```

查看详细的 API 请求和响应日志。

## 联系支持

如果问题持续存在，请检查：
1. DeepSeek 官方文档：https://platform.deepseek.com/
2. 项目 GitHub Issues
3. 联系开发团队
