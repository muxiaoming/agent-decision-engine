# Spring AI + Langfuse 3 演示项目

基于 Spring Boot 3.4.5 + Spring AI 1.0.0 的多模型 AI 应用演示，集成 Langfuse 3 实现全链路可观测性。

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.4.5 | 应用框架 |
| Spring AI | 1.0.0 | AI 模型抽象层 |
| Spring AI Alibaba | 1.0.0.4 | DashScope 集成 + Graph 工作流 |
| Langfuse | 3.x | AI 可观测性平台 |
| Java | 21 | 运行时（虚拟线程） |

## 功能模块

- **多模型聊天**：DeepSeek / MiMo（OpenAI 兼容）/ DashScope，运行时动态切换
- **流式输出**：Flux / SseEmitter / ServerSentEvent 三种实现
- **Function Calling**：天气查询、数学计算工具，自动装配到所有模型
- **RAG 检索增强生成**：DashScope 嵌入模型 + 向量存储 + QuestionAdvisor
- **Graph 工作流**：有向图流程编排，条件分支路由
- **Langfuse 可观测性**：OTLP 协议导出 Trace，支持本地/云端双模式部署

## 目录结构

```
├── src/                          # 源代码
│   ├── main/java/com/example/ai/
│   │   ├── chat/                 # 聊天模块（同步 + 三种流式）
│   │   ├── common/               # 公共组件（路由器、DTO、异常处理）
│   │   ├── graph/                # Graph 工作流模块
│   │   ├── observability/        # Langfuse 可观测性模块
│   │   ├── rag/                  # RAG 检索增强生成模块
│   │   └── tools/                # Function Calling 工具模块
│   ├── main/resources/
│   │   ├── application.yml       # 主配置（三模型提供者）
│   │   ├── application-local.yml # 本地 Langfuse 部署配置
│   │   ├── application-cloud.yml # 云端 Langfuse 配置
│   │   └── docs/                 # RAG 示例文档
│   └── test/                     # 测试代码
├── docker/                       # Docker Compose 配置
│   └── docker-compose-langfuse.yml  # Langfuse v3 完整部署栈
├── specs/                        # Speckit 功能规格文档
│   ├── 001-springai-langfuse3-demo/  # 功能 001 完整规格
│   └── 002-tool-refactor-code-style/ # 功能 002 规格
├── openspec/                     # OpenSpec 变更记录
│   └── changes/                  # 功能变更的设计和任务
└── .specify/                     # Speckit 项目宪章和工具配置
```

## 需求管理：Speckit vs OpenSpec

本项目先后试用了两种 AI 辅助需求管理工具，最终选择 Speckit 作为主要工具。

| 对比维度 | Speckit | OpenSpec |
|----------|---------|----------|
| 工作方式 | 从自然语言生成结构化规格文档 | 探索模式渐进式设计，提议模式快速生成 |
| 产物 | `specs/` 目录下的 spec/plan/tasks/data-model/api 等 | `openspec/changes/` 下的 proposal/design/spec |
| 文档粒度 | 细粒度（独立的 API 契约、数据模型、快速启动指南） | 粗粒度（合并的设计+规格文档） |
| 适用场景 | 需要完整工程化规格的项目 | 快速探索和原型验证 |
| 选择原因 | 文档结构更适合项目展示 | 适合早期需求探索阶段 |

## 快速启动

```bash
# 1. 启动 Langfuse 本地部署
docker compose -f docker/docker-compose-langfuse.yml up -d

# 2. 配置 API 密钥（复制 application-dev.yml 填入真实密钥）
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml

# 3. 启动应用
./mvnw spring-boot:run

# 4. 测试聊天 API
curl -X POST http://localhost:8181/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "你好"}'

# 5. 验证 Langfuse 连接
curl http://localhost:8181/api/observability/health
```
