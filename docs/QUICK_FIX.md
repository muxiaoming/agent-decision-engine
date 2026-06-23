# 🔍 DeepSeek API 配置问题诊断

## ✅ 验证结果

API Key **是有效的**！

```
✅ API 连接成功 (HTTP 200)
✅ 配置文件存在: application-dev.yml
✅ API Key: sk-ec1404d8803544dbb0a3db8b9acb346d
```

---

## ❓ 问题分析

**为什么 API Key 有效，但应用还是报错？**

### 可能原因

1. **应用未使用 dev profile 启动**
   - 配置了 `active: local,dev`
   - 但启动时可能没有正确加载

2. **应用未重启**
   - 修改配置后需要重启应用
   - 旧的配置仍在使用

3. **配置文件加载顺序问题**
   - Spring Boot 加载配置的优先级
   - 环境变量 vs 配置文件

---

## 🔧 快速修复

### 方案 1: 重启应用（推荐）

**Windows (Git Bash):**
```bash
cd "D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo"

# 停止现有应用（如果有）
pkill -f "springai-langfuse3-demo" || true

# 使用 dev profile 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Windows (PowerShell):**
```powershell
cd "D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo"

# 停止现有应用
Get-Process -Name java | Where-Object {$_.CommandLine -like "*springai-langfuse3*"} | Stop-Process -Force

# 启动应用
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**或者直接指定 profile：**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local,dev
```

### 方案 2: 检查启动日志

启动应用后，查看日志确认配置加载：

```bash
# 查看 Spring Boot 启动日志
mvn spring-boot:run 2>&1 | grep -E "(Active profiles|deepseek|api-key)"
```

应该看到：
```
Active profiles: local,dev
```

### 方案 3: 通过环境变量强制指定

```bash
# 设置环境变量（临时）
export SPRING_PROFILES_ACTIVE=local,dev

# 启动应用
mvn spring-boot:run
```

---

## 📋 完整修复步骤

### 步骤 1: 停止现有应用

```bash
# 查找并停止 Java 进程
ps aux | grep java | grep springai
kill -9 <PID>
```

### 步骤 2: 清理并重新编译

```bash
cd "D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo"
mvn clean compile -DskipTests
```

### 步骤 3: 使用 dev profile 启动

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 步骤 4: 验证启动日志

应用启动时应该看到：

```
Started DecisionEngineApplication in X.XXX seconds
Active profiles: local,dev
```

### 步骤 5: 测试接口

```bash
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k"}'
```

**预期**: 6/6 步骤成功 ✅

---

## 🔍 调试配置加载

### 检查应用是否加载了正确的 profile

在 `application.yml` 中添加日志配置：

```yaml
logging:
  level:
    org.springframework: INFO
    com.zhou.ai: DEBUG
```

### 检查配置值

在应用启动时查看日志中的配置：

```bash
mvn spring-boot:run 2>&1 | grep -i "deepseek\|api-key"
```

### 添加配置检查端点

在 Controller 中添加测试端点：

```java
@GetMapping("/config/debug")
public ResponseEntity<Map<String, String>> debugConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("deepseek.api-key", "sk-" + "ec1404d8803544dbb0a3db8b9acb346d".substring(3));
    config.put("profiles.active", Arrays.toString(env.getActiveProfiles()));
    return ResponseEntity.ok(config);
}
```

---

## 📊 配置优先级

Spring Boot 配置优先级（从高到低）：

1. **命令行参数** - `--spring.ai.deepseek.api-key=xxx`
2. **环境变量** - `SPRING_AI_DEEPSEEK_API_KEY`
3. **Profile-specific 文件** - `application-dev.yml` ✅ 当前使用
4. **application.yml** - 默认配置

---

## 🎯 根本原因总结

**API Key 是有效的**，问题是：

1. ❌ 应用可能没有正确加载 `application-dev.yml`
2. ❌ 或者应用重启后没有生效
3. ❌ 或者需要显式指定 profile

**解决方案**:
✅ 使用 `-Dspring-boot.run.profiles=dev` 启动应用
✅ 重启应用以加载新配置

---

## 🚀 快速测试命令

```bash
cd "D:\Program Files\Dev\Workspace\AIProject\springai-langfuse3-demo"

# 1. 停止现有应用
pkill -f "springai-langfuse3-demo" || true

# 2. 清理并编译
mvn clean compile -DskipTests

# 3. 使用 dev profile 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. 等待启动完成后，在另一个终端测试
curl -N -X POST http://localhost:8182/api/investment/decide/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "I want to invest in tech stocks, budget 100k"}'
```

**预期**: 6/6 步骤成功 ✅

---

## 💡 如果仍然失败

### 检查 1: 应用日志

```bash
# 查看详细日志
mvn spring-boot:run 2>&1 | tee startup.log
grep -E "(DeepSeek|api-key|Active profiles)" startup.log
```

### 检查 2: 配置验证端点

```bash
curl http://localhost:8182/api/investment/health
curl http://localhost:8182/api/investment/config/debug  # 如果添加了此端点
```

### 检查 3: 手动测试 API

```bash
# 直接测试 API Key
curl -X POST https://api.deepseek.com/chat/completions \
  -H "Authorization: Bearer sk-ec1404d8803544dbb0a3db8b9acb346d" \
  -H "Content-Type: application/json" \
  -d '{"model":"deepseek-chat","messages":[{"role":"user","content":"test"}],"max_tokens":5}'
```

---

## 📝 相关文档

- `docs/DEEPSEEK_DIAGNOSIS.md` - 完整诊断报告
- `docs/API_KEY_SETUP.md` - API Key 配置指南
- `scripts/verify-config.sh` - 配置验证脚本

---

**诊断日期**: 2026-06-21
**状态**: ✅ API Key 有效，需要正确加载配置
**下一步**: 使用 dev profile 重启应用
