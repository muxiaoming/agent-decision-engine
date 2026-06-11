## 1. Project Skeleton & Maven Dependencies

- [ ] 1.1 创建 Maven 项目结构：`pom.xml`、`src/main/java`、`src/main/resources`、`src/test/java`
- [ ] 1.2 配置 `pom.xml`：Spring Boot 3.x parent、Java 21 编译、Spring AI BOM、Spring AI Alibaba BOM
- [ ] 1.3 添加核心依赖：`spring-boot-starter-web`、`spring-boot-starter-actuator`、`micrometer-tracing-bridge-otel`、`opentelemetry-exporter-otlp`
- [ ] 1.4 添加模型依赖：`spring-ai-starter-model-deepseek`、`spring-ai-starter-model-openai`（MiMo 兼容）、`spring-ai-alibaba-starter`（DashScope）
- [ ] 1.5 添加 RAG 依赖：`spring-ai-rag`、`spring-ai-vector-store`（SimpleVectorStore）
- [ ] 1.6 添加 Graph 依赖：`spring-ai-alibaba-graph-core`
- [ ] 1.7 创建 `application.yml` 模板，包含多模型配置、Langfuse OTLP 配置、RAG 配置占位符

## 2. Configuration & Bean Registration

- [ ] 2.1 创建 `ModelConfig` 配置类：为 DeepSeek、MiMo、DashScope 分别注册 `ChatModel` 和 `ChatClient` Bean（使用 `@Qualifier` 区分）
- [ ] 2.2 创建 `RagConfig` 配置类：注册 `SimpleVectorStore`、`TextSplitter`、`DocumentReader` Bean
- [ ] 2.3 创建 `FunctionCallingConfig` 配置类：注册工具函数 Bean（天气查询、计算器等示例）
- [ ] 2.4 创建 `GraphConfig` 配置类：定义 StateGraph 工作流 Bean（顺序、并行、条件分支）

## 3. Langfuse 3 / OTel Observability

- [ ] 3.1 创建 `LangfuseProperties` 配置属性类：读取 `app.langfuse.*` 配置（mode、public-key、secret-key、cloud-region、self-hosted-url），自动生成 Basic Auth Header 和 OTLP endpoint
- [ ] 3.2 创建 `application-langfuse-cloud.yml` Profile：配置 Langfuse Cloud OTLP 端点（默认 EU，支持 US/JP region 切换）
- [ ] 3.3 创建 `application-langfuse-local.yml` Profile：配置本地自部署 Langfuse OTLP 端点 `http://localhost:3000/api/public/otel`
- [ ] 3.4 创建 `docker-compose.langfuse.yml`：Langfuse Server + PostgreSQL + Redis 一键本地部署，预设项目 Public/Secret Key 环境变量
- [ ] 3.5 配置 OTLP Exporter：在主 `application.yml` 中通过 `LangfuseProperties` 动态组装 `OTEL_EXPORTER_OTLP_ENDPOINT` 和 `OTEL_EXPORTER_OTLP_HEADERS`
- [ ] 3.6 验证 Cloud 模式：`--spring.profiles.active=langfuse-cloud` 启动后调用 `/api/chat`，在 Langfuse Cloud 确认 Trace 自动生成
- [ ] 3.7 验证 Local 模式：启动 `docker-compose.langfuse.yml`，`--spring.profiles.active=langfuse-local` 启动后在本地 Langfuse UI 确认 Trace 自动生成

## 4. Multi-Model Chat

- [ ] 4.1 创建 `ChatController`：实现 `POST /api/chat`（同步）和 `GET /api/chat/stream`（SSE 流式），支持 `model` 参数路由
- [ ] 4.2 创建 `ChatService`：封装模型路由逻辑，通过 Map<model-name, ChatClient> 动态分发
- [ ] 4.3 实现错误处理：无效模型名称返回 400 + 可用模型列表
- [ ] 4.4 编写单元测试：`ChatServiceTest` 验证模型路由和错误处理

## 5. RAG Retrieval

- [ ] 5.1 创建 `RagController`：实现 `POST /api/rag/query`（RAG 查询）和 `POST /api/rag/documents`（文档上传）
- [ ] 5.2 创建 `RagService`：封装文档加载 → 文本分割 → 向量化 → 检索 → 增强生成全流程
- [ ] 5.3 准备示例文档：在 `src/main/resources/docs/` 下放入 2-3 个示例 Markdown/TXT 文档
- [ ] 5.4 实现 `RetrievalAugmentationAdvisor` 集成：使用 Spring AI RAG 模块的标准 Advisor 链
- [ ] 5.5 编写集成测试：`RagServiceTest` 验证文档索引和查询流程

## 6. Function Calling

- [ ] 6.1 创建示例工具函数：`WeatherFunction`（查询天气）、`CalculatorFunction`（数学计算），使用 `@Description` 注解
- [ ] 6.2 创建 `FunctionCallController`：实现 `POST /api/function-call`，将用户消息与工具函数一起发送给 LLM
- [ ] 6.3 创建 `FunctionCallService`：封装工具注册与调用逻辑，支持多工具并行注册
- [ ] 6.4 编写测试：`FunctionCallServiceTest` 验证工具调用和 Trace 中的工具 Span 记录

## 7. Graph Workflow

- [ ] 7.1 实现顺序工作流示例：`SequentialWorkflow` — 输入 → 分析 → 总结 → 输出
- [ ] 7.2 实现条件分支工作流示例：`ConditionalWorkflow` — 输入 → 分类 → 正面/负面分支 → 输出
- [ ] 7.3 实现并行工作流示例：`ParallelWorkflow` — 输入 → 并行分析 → 合并结果 → 输出
- [ ] 7.4 创建 `GraphController`：实现 `POST /api/graph/workflow`，支持 `workflow` 参数选择工作流类型
- [ ] 7.5 创建 `GraphService`：封装 StateGraph 编译与执行逻辑，管理 OverAllState 生命周期
- [ ] 7.6 验证 Graph Trace：确认 Langfuse 中每个 Graph 节点有独立 Span

## 8. Integration Testing & Documentation

- [ ] 8.1 编写端到端集成测试：覆盖 Chat、RAG、Function Call、Graph 四个 API 端点
- [ ] 8.2 创建 `curl` 示例脚本：`examples/curl-examples.sh` 包含所有 API 的调用示例
- [ ] 8.3 编写 `README.md`：项目说明、快速启动指南（Cloud 模式 + 本地模式）、配置说明、API 文档
