## ADDED Requirements

### Requirement: OTLP Exporter configuration for Langfuse

The system SHALL configure OpenTelemetry OTLP HTTP Exporter to send traces to the Langfuse OTLP endpoint via environment variables or `application.yml`.

#### Scenario: Configure via environment variables
- **WHEN** `OTEL_EXPORTER_OTLP_ENDPOINT` and `OTEL_EXPORTER_OTLP_HEADERS` environment variables are set
- **THEN** all OpenTelemetry traces are exported to the specified Langfuse endpoint

#### Scenario: Configure via application.yml
- **WHEN** `application.yml` contains `app.langfuse.endpoint`, `app.langfuse.public-key`, and `app.langfuse.secret-key`
- **THEN** the application constructs the OTLP headers (Basic Auth) and endpoint at startup

### Requirement: Dual deployment mode support (Cloud + Self-hosted)

The system SHALL support both Langfuse Cloud and self-hosted Langfuse (Docker Compose) as first-class deployment targets, each with its own dedicated configuration profile.

#### Scenario: Connect to Langfuse Cloud (EU region)
- **WHEN** `application.yml` sets `app.langfuse.mode=cloud` with `public-key` and `secret-key`
- **THEN** traces are exported to `https://cloud.langfuse.com/api/public/otel` with Basic Auth header

#### Scenario: Connect to Langfuse Cloud (US region)
- **WHEN** `application.yml` sets `app.langfuse.mode=cloud` and `app.langfuse.cloud-region=us`
- **THEN** traces are exported to `https://us.cloud.langfuse.com/api/public/otel`

#### Scenario: Connect to Langfuse Cloud (Japan region)
- **WHEN** `application.yml` sets `app.langfuse.mode=cloud` and `app.langfuse.cloud-region=jp`
- **THEN** traces are exported to `https://jp.cloud.langfuse.com/api/public/otel`

#### Scenario: Connect to self-hosted Langfuse
- **WHEN** `application.yml` sets `app.langfuse.mode=self-hosted` with `app.langfuse.self-hosted-url=http://localhost:3000`
- **THEN** traces are exported to `http://localhost:3000/api/public/otel` with Basic Auth header

### Requirement: Self-hosted Langfuse Docker Compose setup

The system SHALL provide a `docker-compose.yml` that spins up a complete local Langfuse environment (Langfuse server + PostgreSQL + Redis) for development and evaluation purposes.

#### Scenario: Start local Langfuse with Docker Compose
- **WHEN** developer runs `docker compose -f docker-compose.langfuse.yml up -d`
- **THEN** Langfuse UI is accessible at `http://localhost:3000` and OTLP endpoint at `http://localhost:3000/api/public/otel`

#### Scenario: Local Langfuse with seeded project keys
- **WHEN** Langfuse container starts with environment variables `LANGFUSE_INIT_PROJECT_PUBLIC_KEY` and `LANGFUSE_INIT_PROJECT_SECRET_KEY` set
- **THEN** the project is pre-created with the specified keys, matching `application.yml` self-hosted configuration

### Requirement: Configuration profile switching

The system SHALL use Spring profiles (`langfuse-cloud` and `langfuse-local`) to switch between Cloud and self-hosted modes without changing application code.

#### Scenario: Activate Cloud profile
- **WHEN** application starts with `--spring.profiles.active=langfuse-cloud`
- **THEN** OTLP Exporter targets Langfuse Cloud endpoint

#### Scenario: Activate local profile
- **WHEN** application starts with `--spring.profiles.active=langfuse-local`
- **THEN** OTLP Exporter targets local self-hosted endpoint at `http://localhost:3000/api/public/otel`

### Requirement: Automatic Spring AI trace instrumentation

The system SHALL automatically capture ChatModel calls, embeddings, and retriever operations as OpenTelemetry spans without manual instrumentation code.

#### Scenario: Chat call traced automatically
- **WHEN** a `/api/chat` request triggers a DeepSeek API call
- **THEN** a trace is created with spans for: HTTP request → ChatModel call → LLM generation → response

#### Scenario: RAG pipeline traced automatically
- **WHEN** a `/api/rag/query` request triggers retrieval and generation
- **THEN** a trace is created with spans for: embedding → vector search → prompt construction → LLM generation

### Requirement: Langfuse trace enrichment with metadata

The system SHALL attach metadata (model name, user ID, session ID) to traces so they are searchable and filterable in Langfuse.

#### Scenario: Model name in trace metadata
- **WHEN** a chat request uses the DashScope model
- **THEN** the Langfuse trace contains `gen_ai.request.model: qwen-plus` in its attributes

#### Scenario: Session grouping
- **WHEN** multiple requests share the same `session_id` header
- **THEN** Langfuse groups these traces under a single session

