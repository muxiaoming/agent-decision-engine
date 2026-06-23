# 🔍 DeepSeek API 诊断报告

## 📋 诊断结果

### ❌ 主要问题：DeepSeek API Key 未设置

```
检查环境变量:
   ❌ DEEPSEEK_API_KEY 未设置

检查 application.yml 配置:
   ⚠️  使用默认配置 (your-deepseek-api-key)
```

---

## 🎯 DeepSeek 是否支持？

### ✅ **支持，但需要 API Key**

1. **配置存在** - `application.yml` 中有 DeepSeek 配置
2. **Skills 使用 DeepSeek** - `SkillsAgentConfig.java` 第41行明确使用 `deepSeekChatModel`
3. **但 API Key 缺失** - 使用了默认的 "your-deepseek-api-key"

### 代码验证

```java
// SkillsAgentConfig.java
@Bean
public ReactAgent skillsAgent(
        @Qualifier("deepSeekChatModel") ChatModel chatModel,  // ← 使用 DeepSeek
        ...) {
    return ReactAgent.builder()
            .model(chatModel)
            .build();
}
```

---

## 📊 影响范围

| 步骤 | 模块 | 是否需要 AI | 当前状态 | 修复方法 |
|------|------|------------|---------|---------|
| Step 1 | 问题感知 (Skills) | ✅ 是 (DeepSeek) | ❌ 失败 | 设置 API Key |
| Step 2 | 知识检索 (RAG) | ✅ 是 (DeepSeek) | ❌ 失败 | 设置 API Key |
| Step 3 | 数据获取 (Tools) | ❌ 否 | ✅ 成功 | - |
| Step 4 | 推理分析 (Skills) | ✅ 是 (DeepSeek) | ❌ 失败 | 设置 API Key |
| Step 5 | 决策生成 (Skills) | ✅ 是 (DeepSeek) | ❌ 失败 | 设置 API Key |
| Step 6 | 流程编排 (Graph) | ❌ 否 | ✅ 成功 | - |

**总结**: 4/6 步骤需要 DeepSeek API，当前只有 2/6 步骤工作

---

## ✅ 解决方案

### 方案 1: 设置环境变量（推荐）

**Windows (Git Bash):**
```bash
# 1. 获取 API Key
#    访问 https://platform.deepseek.com/
#    注册/登录 → 生成 API Key (格式: sk-...)

# 2. 设置环境变量
export DEEPSEEK_API_KEY=sk-your-real-api-key

# 3. 验证设置
echo $DEEPSEEK_API_KEY  # 应该显示 sk-...

# 4. 重启应用
mvn spring-boot:run

# 5. 重新测试
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k"}'
```

**Windows (PowerShell):**
```powershell
# 设置环境变量
$env:DEEPSEEK_API_KEY="sk-your-real-api-key"

# 验证
echo $env:DEEPSEEK_API_KEY

# 重启应用
mvn spring-boot:run
```

**Linux/macOS:**
```bash
# 永久设置
echo 'export DEEPSEEK_API_KEY=sk-your-real-api-key' >> ~/.bashrc
source ~/.bashrc

# 或临时设置
export DEEPSEEK_API_KEY=sk-your-real-api-key
```

### 方案 2: 修改 application.yml

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    deepseek:
      api-key: sk-your-real-api-key  # 直接写入 API Key
```

⚠️ **安全提醒**: 不要将 API Key 提交到 Git！

### 方案 3: 创建 .env 文件

在项目根目录创建 `.env` 文件：

```bash
DEEPSEEK_API_KEY=sk-your-real-api-key
```

然后使用 Maven 插件加载。

---

## 🧪 验证修复

### 1. 重启应用前检查

```bash
echo $DEEPSEEK_API_KEY  # 应该显示 sk-...
```

### 2. 重启应用

```bash
cd "D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo"
mvn spring-boot:run
```

### 3. 测试完整流程

```bash
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k"}'
```

**预期结果**:
```
Step 1: 问题感知 (Skills)     ✅ 成功
Step 2: 知识检索 (RAG)        ✅ 成功
Step 3: 数据获取 (Tools)      ✅ 成功
Step 4: 推理分析 (Skills)     ✅ 成功
Step 5: 决策生成 (Skills)     ✅ 成功
Step 6: 流程编排 (Graph)      ✅ 成功
```

**总计**: 6/6 步骤成功 ✅

---

## 🔧 只测试 Tools 和 Graph（不需要 API Key）

如果你想验证流程编排是否正常，可以禁用 AI 步骤：

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

**预期结果**:
```
Step 1: 问题感知 (Skills)     ⏭️  跳过
Step 2: 知识检索 (RAG)        ⏭️  跳过
Step 3: 数据获取 (Tools)      ✅ 成功 (AAPL: 178.52)
Step 4: 推理分析 (Skills)     ⏭️  跳过
Step 5: 决策生成 (Skills)     ⏭️  跳过
Step 6: 流程编排 (Graph)      ✅ 成功 (分类: technical)
```

**总计**: 2/6 步骤成功 ✅

---

## 📊 API Key 获取步骤

### 1. 访问 DeepSeek 平台

- 网址: https://platform.deepseek.com/
- 注册/登录账户

### 2. 生成 API Key

- 进入控制台
- 选择 "API Keys"
- 点击 "Create API Key"
- 复制生成的 Key（格式: `sk-...`）

### 3. 测试 API Key

```bash
curl -X POST https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer sk-your-real-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "Hello"}],
    "max_tokens": 10
  }'
```

**预期**: 返回 JSON 响应，包含 `"choices"` 字段

---

## 📝 相关文件

- `scripts/test-deepseek-api.sh` - API 诊断脚本
- `docs/API_KEY_SETUP.md` - API Key 配置指南
- `docs/BUG_FIX_REPORT.md` - Bug 修复报告

---

## 💡 常见问题

### Q1: API Key 设置后仍然报错？

**可能原因**:
1. API Key 无效或已过期
2. 账户余额不足
3. 网络连接问题

**解决方法**:
```bash
# 测试 API Key
curl -X POST https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"test"}],"max_tokens":5}'

# 检查网络
ping api.deepseek.com
```

### Q2: 是否可以使用其他 AI 模型？

**可以！** 项目支持：
- DeepSeek (当前配置)
- OpenAI / MiMo (配置在 application.yml)
- DashScope (阿里云)

修改配置即可切换模型。

### Q3: 如何在不设置 API Key 的情况下测试？

只启用 Tools 和 Graph：
```json
{
  "message": "Get stock price",
  "enableRAG": false,
  "enableTools": true,
  "enableGraph": true
}
```

---

## 🎓 技术细节

### 为什么是"支持但需要配置"？

1. **配置存在** - `application.yml` 有 DeepSeek 配置
2. **代码使用** - `SkillsAgentConfig` 使用 `deepSeekChatModel`
3. **默认值** - API Key 使用 `${DEEPSEEK_API_KEY:your-deepseek-api-key}`
4. **环境变量** - 优先从环境变量读取
5. **当前状态** - 环境变量未设置，使用了默认值（无效）

### 错误信息解读

```
Exception: Error while extracting response for type
[org.springframework.ai.deepseek.api.DeepSeekApi$ChatCompletion]
and content type [application/json]
```

**含义**: Spring AI 尝试解析 DeepSeek API 响应，但：
- API 返回了 401 (Unauthorized) 或其他错误
- 响应格式不是有效的 ChatCompletion
- 原因：API Key 缺失或无效

---

## 📈 预期修复后效果

### 修复前（当前）
```
✅ Step 3: Tools    (不需要 AI)
✅ Step 6: Graph    (不需要 AI)
❌ Step 1: Skills   (需要 DeepSeek)
❌ Step 2: RAG      (需要 DeepSeek)
❌ Step 4: Skills   (需要 DeepSeek)
❌ Step 5: Skills   (需要 DeepSeek)

总计: 2/6 成功
```

### 修复后（设置 API Key）
```
✅ Step 1: Skills   (问题感知)
✅ Step 2: RAG      (知识检索)
✅ Step 3: Tools    (数据获取)
✅ Step 4: Skills   (推理分析)
✅ Step 5: Skills   (决策生成)
✅ Step 6: Graph    (流程编排)

总计: 6/6 成功
```

---

## ✅ 下一步

1. ✅ 获取 DeepSeek API Key
2. ✅ 设置环境变量: `export DEEPSEEK_API_KEY=sk-...`
3. ✅ 重启应用: `mvn spring-boot:run`
4. ✅ 重新测试
5. ✅ 验证 6/6 步骤成功

---

**诊断日期**: 2026-06-21
**根本原因**: DeepSeek API Key 未设置
**解决状态**: 需要设置 API Key
**预计耗时**: 5-10 分钟
