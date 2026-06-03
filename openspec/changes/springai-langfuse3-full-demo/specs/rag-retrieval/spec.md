## ADDED Requirements

### Requirement: Document ingestion and vectorization

The system SHALL support loading documents from classpath resources, splitting them into chunks, and storing embeddings in a vector store.

#### Scenario: Load and index documents on startup
- **WHEN** application starts with documents in `classpath:docs/` directory
- **THEN** documents are loaded, split into chunks, embedded, and stored in the configured vector store

#### Scenario: Manual document upload via API
- **WHEN** client sends `POST /api/rag/documents` with a file upload
- **THEN** the document is parsed, chunked, embedded, and indexed into the vector store

### Requirement: RAG query with retrieval-augmented generation

The system SHALL expose `POST /api/rag/query` that performs semantic search against the vector store and uses retrieved context to augment the LLM prompt.

#### Scenario: Successful RAG query
- **WHEN** client sends `POST /api/rag/query` with `{"question": "What is Spring AI?", "model": "deepseek"}`
- **THEN** the system retrieves relevant document chunks, constructs an augmented prompt, and returns the LLM response with source citations

#### Scenario: No relevant documents found
- **WHEN** client sends a query with no matching documents in the vector store
- **THEN** the system returns a response indicating no relevant context was found, optionally falling back to direct LLM answer

### Requirement: Configurable text splitting strategy

The system SHALL support configurable chunk size and overlap for text splitting.

#### Scenario: Custom chunk parameters
- **WHEN** `application.yml` sets `app.rag.chunk-size=500` and `app.rag.chunk-overlap=50`
- **THEN** documents are split with 500-token chunks and 50-token overlap

### Requirement: Vector store pluggability

The system SHALL default to SimpleVectorStore (in-memory) and support switching to PgVector via configuration.

#### Scenario: Default in-memory vector store
- **WHEN** no external vector database is configured
- **THEN** SimpleVectorStore is used for vector storage

#### Scenario: Switch to PgVector
- **WHEN** `application.yml` sets `app.rag.vector-store=pgvector` with valid PostgreSQL connection
- **THEN** PgVector is used as the vector store backend
