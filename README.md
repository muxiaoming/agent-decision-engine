# Spring AI + Langfuse 3 演示项目

基于 Spring Boot 3.4.5 + Spring AI 1.0.0 的多模型 AI 应用演示，集成 Langfuse 3 实现全链路可观测性。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.5 | 应用框架 |
| Spring AI | 1.1.2 | AI 模型抽象层 |
| Spring AI Alibaba | 1.1.2.2 | DashScope 集成 + Graph 工作流 |
| Langfuse | 3.x | AI 可观测性平台（本地/云端） |
| Java | 21 | 运行时（虚拟线程） |
| OpenTelemetry | 2.17.0 | 指标和追踪导出 |
| Knife4j | 4.5.0 | OpenAPI 文档 |

## 核心功能

### 🤖 多模型聊天
- **支持模型**: DeepSeek、Xiaomi MiMo、DashScope Qwen
- **动态切换**: 通过 `modelName` 参数实时切换模型
- **流式输出**: 支持 Flux/SSE 三种流式实现

### 🔍 RAG 检索增强生成
- **知识库摄取**: 上传文档自动分割并存入向量存储
- **语义搜索**: 基于向量相似度检索相关知识
- **问答增强**: 使用检索结果增强 LLM 回答准确性

### 🔧 Function Calling
- **天气查询工具**: 查询指定城市天气信息
- **数学计算工具**: 执行基本数学运算
- **自动装配**: 工具自动注册到所有模型客户端

### 📊 Langfuse 可观测性
- **全链路追踪**: 从用户请求到模型响应的完整追踪
- **指标采集**: 提示词 token 用量、响应时间、成功率
- **调试模式**: 支持本地和云端 Langfuse 双模式
- **健康检查**: `/api/observability/health` 验证连接状态

### 🎯 Graph 工作流
- **流程编排**: 有向图（DAG）流程管理
- **条件分支**: 支持基于条件的路由分支
- **状态管理**: 工作流状态追踪和日志记录

### 👨‍💼 Agent 框架
- **Skills 动态加载**: 基于描述的技能自动发现
- **多工具协调**: 统一的工具调用接口
- **执行追踪**: 所有工具调用在 Langfuse 中可追踪

## 快速启动

### 前提条件
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### 1. 克隆和构建

```bash
git clone https://github.com/your-repo/springai-langfuse3-demo.git
cd springai-langfuse3-demo
mvn clean install -DskipTests
```

### 2. 启动 Langfuse

```bash
docker compose -f docker/docker-compose-langfuse.yml up -d
```

访问 `http://localhost:3000` 完成 Langfuse 初始化。

### 3. 配置 API 密钥

创建 `src/main/resources/application-dev.yml`：

```yaml
spring:
  ai:
    deepseek:
      api-key: your-deepseek-key
    openai:
      api-key: your-mimo-key
    dashscope:
      api-key: your-dashscope-key

otel:
  exporter:
    otlp:
      headers:
        Authorization: "Basic your-langfuse-credentials"
```

### 4. 启动应用

```bash
# 使用 dev 配置启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

应用启动后访问:
- **API 文档**: `http://localhost:8182/swagger-ui.html`
- **健康检查**: `http://localhost:8182/api/observability/health`

### 5. 测试 API

**同步聊天**
```bash
curl -X POST http://localhost:8182/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下你自己"}'
```

**流式聊天**
```bash
curl -X POST http://localhost:8182/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "写一首诗"}'
```

**查询可用模型**
```bash
curl http://localhost:8182/api/chat/models
```

**RAG 问答**
```bash
curl -X POST http://localhost:8182/api/rag/ask \
  -H "Content-Type: application/json" \
  -d '{"message": "Spring AI 是什么？", "model": "deepSeekChatModel"}'
```

**文档摄取**
```bash
curl -X POST http://localhost:8182/api/rag/documents \
  -F "file=@your-document.txt"
```

## 配置选项

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | - |
| `MIMO_API_KEY` | Xiaomi MiMo API 密钥 | - |
| `DASHSCOPE_API_KEY` | DashScope API 密钥 | - |
| `LANGFUSE_MODE` | Langfuse 模式 (local/cloud) | `local` |

### Spring Profiles

| Profile | 说明 |
|---------|------|
| `local` | 本地 Langfuse 部署 |
| `cloud` | 云端 Langfuse |
| `dev` | 开发配置（不提交到 git） |

### 运行时配置

```bash
# 指定启动 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local

# 切换端口
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  Client (Postman / Frontend / curl)                        │
└──────────┬──────────────────────────────┬───────────────────┘
           │                              │
           ▼                              ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ ChatController   │  │ RagController    │  │ GraphController  │
│ (同步/流式)      │  │ (RAG 问答)       │  │ (工作流执行)     │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                     │
         └─────────────────────┼─────────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   ModelRouter       │
                    │ (动态模型选择)      │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ DeepSeek     │  │ MiMo         │  │ DashScope    │
│ (OpenAI兼容) │  │ (OpenAI兼容) │  │ (专有API)   │
└──────────────┘  └──────────────┘  └──────────────┘
                               │
                    ┌──────────▼──────────┐
                    │ ToolCallbackProvider │
                    │ (自动装配工具)       │
                    └──────────┬──────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ WeatherTool  │  │ Calculator   │  │ 其他工具     │
└──────────────┘  └──────────────┘  └──────────────┘
                               │
                    ┌──────────▼──────────┐
                    │ Langfuse (OTLP)     │
                    │ (可观测性平台)       │
                    └─────────────────────┘
```

## 文档

- **功能规格**: `specs/001-springai-langfuse3-demo/spec.md`
- **API 契约**: `specs/001-springai-langfuse3-demo/contracts/api.md`
- **数据模型**: `specs/001-springai-langfuse3-demo/data-model.md`
- **实现计划**: `specs/001-springai-langfuse3-demo/plan.md`
- **快速启动**: `specs/001-springai-langfuse3-demo/quickstart.md`

## 开发指南

### 添加新模型

1. 在 `application.yml` 中添加模型配置
2. Spring AI 自动创建 `ChatModel` Bean
3. 自动被 `ModelRouter` 发现和注册

### 添加新工具

1. 创建 `@Component` 类并使用 `@Tool` 注解
2. 工具自动注册到 `ToolCallbackProvider`
3. 自动装配到所有模型客户端

### 集成新的 Langfuse 特性

参考 Langfuse 3.x 文档和 `observability` 包中的实现。

## 故障排除

### 连接 Langfuse 失败

```bash
# 检查 Langfuse 是否运行
docker compose -f docker/docker-compose-langfuse.yml ps

# 查看应用日志
docker logs springai-langfuse3-demo

# 验证健康状态
curl http://localhost:8182/api/observability/health
```

### 模型 API 密钥无效

```bash
# 检查环境变量
echo $DEEPSEEK_API_KEY

# 或查看 dev 配置
cat src/main/resources/application-dev.yml
```

### 向量存储数据丢失

`SimpleVectorStore` 存储在内存中，重启应用会丢失数据。切换到生产向量存储：

- **Chroma**: `spring-ai-starter-vector-store-chroma`
- **Milvus**: `spring-ai-starter-vector-store-milvus`
- **PostgreSQL**: `spring-ai-starter-vector-store-pgvector`

## 贡献

欢迎通过 Issue 和 Pull Request 贡献代码。

## 许可

MIT License

## 相关资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Langfuse 文档](https://langfuse.com/docs)
- [DashScope 文档](https://dashscope.console.aliyun.com/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
