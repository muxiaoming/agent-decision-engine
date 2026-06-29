# API 路径配置说明

## 配置方式

所有 API 路径前缀统一配置在 `application.yml` 中：

```yaml
server:
  port: 8182
  servlet:
    context-path: /api
```

## 路径映射规则

### 配置层
- `context-path: /api` - 所有 Controller 自动添加此前缀

### Controller 层
- 每个 Controller 使用相对路径，如 `@RequestMapping("/chat")`
- **不要**在 Controller 上写 `/api` 前缀

### 最终路径
```
context-path + Controller路径 = 完整路径

/api + /chat = /api/chat
/api + /investment = /api/investment
/api + /skills = /api/skills
/api + /rag = /api/rag
...
```

## 完整 API 路由表

| Controller | RequestMapping | 完整路径 |
|-----------|---------------|---------|
| ChatController | `/chat` | `/api/chat` |
| FluxStreamController | `/chat/flux` | `/api/chat/flux` |
| SseEmitterController | `/chat/emitter` | `/api/chat/emitter` |
| SseStreamController | `/chat/sse` | `/api/chat/sse` |
| GraphController | `/graph` | `/api/graph` |
| LangfuseTestController | `/observability` | `/api/observability` |
| RagController | `/rag` | `/api/rag` |
| SkillsAgentController | `/skills` | `/api/skills` |
| ToolChatController | `/tools` | `/api/tools` |
| InvestmentDecisionController | `/investment` | `/api/investment` |

## 添加新 Controller 的规范

```java
@RestController
@RequestMapping("/your-module")  // ✅ 正确：使用相对路径
public class YourController {
    // 实际路径: /api/your-module
}

@RestController
@RequestMapping("/api/your-module")  // ❌ 错误：不要重复添加 /api
public class YourController {
    // 实际路径: /api/api/your-module (重复了!)
}
```

## 前端调用示例

### Vite 开发环境
```typescript
// vite.config.ts
proxy: {
  '/engine': {
    target: 'http://localhost:8182',
    rewrite: (path) => path.replace(/^\/engine/, '/api'),
  },
}

// API 调用
fetch('/engine/investment/decide')  // → http://localhost:8182/api/investment/decide
```

### 生产环境
```typescript
// 直接调用完整路径
fetch('http://localhost:8182/api/investment/decide')
```

## 切换 API 前缀

只需修改 `application.yml` 中的 `context-path`：

```yaml
# 示例 1: 使用 /v1 前缀
server:
  servlet:
    context-path: /v1

# 示例 2: 使用 /rest 前缀
server:
  servlet:
    context-path: /rest

# 示例 3: 移除前缀（直接在根路径）
server:
  servlet:
    context-path: /
```

所有 Controller 的路径会自动适配，无需修改代码。

## 优势

✅ **统一配置** - API 前缀集中管理，一处修改全局生效  
✅ **避免重复** - Controller 只需定义相对路径  
✅ **灵活切换** - 可随时通过配置切换 API 前缀  
✅ **易于维护** - 清晰的职责分离，配置与代码分离  
