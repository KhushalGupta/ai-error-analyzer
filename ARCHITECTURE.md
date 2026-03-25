# AI Error Analyzer — Architecture

This document describes the system structure, major components, and how data flows through the application.

## Overview

The project is a **full-stack RAG (Retrieval-Augmented Generation)** tool for analyzing error logs:

- **Frontend:** Angular 19 SPA that posts logs to the API and displays analysis.
- **Backend:** Spring Boot 3.4 with **Spring AI**, **Ollama** (local LLM and embeddings, no cloud API key), and **PostgreSQL + pgvector** for vector storage and similarity search.
- **Operations:** Spring Boot Actuator + Micrometer Prometheus registry; optional **Docker Compose** for Postgres, Prometheus, and Grafana.

---

## Repository layout

```
AI Error Analyzer/
├── ARCHITECTURE.md          # This file
├── docker-compose.yml       # Postgres (pgvector), Prometheus, Grafana
├── backend/                 # Spring Boot API
│   ├── pom.xml
│   ├── docs/knowledge-base.md   # Markdown ingested into the vector store at startup
│   └── src/main/java/com/ai/error_analyzer/
│       ├── ErrorAnalyzerApplication.java
│       ├── controller/      # REST: /api/analyze, /api/health
│       ├── service/         # ErrorAnalyzerService, DocumentIngestionService
│       ├── dto/             # Records (AnalyzeRequest, AnalyzeResponse)
│       └── repository/      # Reserved for JPA repositories
└── frontend/                # Angular 19 UI
    ├── package.json
    ├── proxy.conf.json      # Dev: /api → http://localhost:8080
    └── src/app/
        ├── app.component.*  # Root UI (signals, OnPush)
        └── services/analyze.service.ts
```

---

## Technology stack

### Backend (`backend/pom.xml`)

| Area | Choices |
|------|---------|
| Runtime | Java 21, Spring Boot 3.4 |
| HTTP | `spring-boot-starter-web`, `spring-boot-starter-validation` |
| AI | Spring AI BOM 1.0; **Ollama** (`spring-ai-starter-model-ollama`) for chat + embeddings |
| Vector DB | **PGVector** (`spring-ai-starter-vector-store-pgvector`) backed by PostgreSQL |
| Persistence | `spring-boot-starter-data-jpa`, PostgreSQL JDBC driver |
| RAG ingestion | `spring-ai-markdown-document-reader` (markdown → documents) |
| Resilience | Resilience4j (`resilience4j-spring-boot3` + AOP) — circuit breaker on LLM calls |
| Observability | `spring-boot-starter-actuator`, `micrometer-registry-prometheus` |
| Tests | H2 (in-memory), Spring Test; AI/vector disabled in test profile |

### Frontend (`frontend/package.json`)

| Area | Choices |
|------|---------|
| Framework | Angular 19 (standalone components, signals in app code) |
| HTTP | `@angular/common/http` via `AnalyzeService` |
| SSR | `@angular/ssr`, Express — optional production path |

---

## Runtime topology and ports

| Service | Default port | Notes |
|---------|--------------|--------|
| Angular dev server | 4200 | `ng serve`; proxies `/api` to backend |
| Spring Boot API | 8080 | `server.port` |
| Ollama | 11434 | `spring.ai.ollama.base-url` |
| PostgreSQL (Compose) | **5433** → container 5432 | Avoids conflict with a local Postgres on 5432 |
| Prometheus (Compose) | 9090 | Scrapes metrics from configured targets |
| Grafana (Compose) | 3000 | Dashboards; default admin password set in compose |

---

## PostgreSQL and pgvector

- **`spring.datasource.*` in `application.yml`** defines the **JDBC connection** to PostgreSQL. That is the single data source used by Spring Boot.
- **pgvector** is a **PostgreSQL extension** (included in the `pgvector/pgvector` image). It enables storing **embedding vectors** in a table and running **similarity** queries (e.g. cosine, HNSW index).
- Spring AI’s **`VectorStore`** (PGVector implementation) uses the **same** `DataSource` to insert rows at ingestion time and query them at analyze time. It does not use a separate database protocol.

---

## Data flow

### Startup

1. Spring Boot creates the `DataSource` and starts JPA (if entities exist) and Actuator.
2. Spring AI configures **Ollama** `EmbeddingModel` and `ChatModel` / `ChatClient.Builder`, and **PGVector** `VectorStore` (dimensions and index settings from `spring.ai.vectorstore.pgvector`).
3. **`DocumentIngestionService`** (when `VectorStore` is available) reads `docs/knowledge-base.md`, splits content (`MarkdownDocumentReader`, `TokenTextSplitter`), embeds chunks, and **`vectorStore.write(...)`** into Postgres.

### User request: `POST /api/analyze`

1. **Angular** — User submits a log; `AnalyzeService` posts `{ "log": "..." }` to `/api/analyze` (dev proxy forwards to port 8080).
2. **`AnalyzeController`** — Validates input, delegates to **`ErrorAnalyzerService`** when present (`Optional` / degraded mode if Ollama is unavailable).
3. **Retrieval** — `ErrorAnalyzerService` calls **`vectorStore.similaritySearch(...)`** with the log text. The query is embedded; pgvector returns top-k similar chunks from the knowledge base.
4. **Generation** — **`ChatClient`** sends a prompt with the log + retrieved context to **Ollama** (`llama3.2`).
5. **Resilience** — **`@CircuitBreaker(name = "llm")`** wraps the LLM path; failures can trigger a fallback response instead of hanging.
6. **Response** — JSON `AnalyzeResponse` (`analysis`, `relatedErrors`) back to the UI.

---

## Key backend classes

| Class | Responsibility |
|-------|----------------|
| `AnalyzeController` | HTTP API; optional degraded response when AI stack is down |
| `ErrorAnalyzerService` | RAG + chat; circuit breaker on LLM |
| `DocumentIngestionService` | ETL: markdown file → split → vector store |
| `HealthController` | Liveness-style endpoint |

---

## Observability

- **Actuator** exposes `health`, `info`, Prometheus scrape at **`/actuator/prometheus`** (see `management.endpoints.web.exposure` in `application.yml`).
- **Micrometer** tags metrics with `application: ai-error-analyzer`.
- **Docker Compose** runs Prometheus and Grafana; **prometheus scrape targets** must match where the Spring Boot app runs (e.g. `host.docker.internal:8080` for a local JVM, or a service name in a full Docker network). The sample `frontend/prometheus.yml` illustrates job names and paths; adjust targets for your environment.

---

## Configuration files

| File | Purpose |
|------|---------|
| `backend/src/main/resources/application.yml` | Datasource, Ollama, PGVector, Resilience4j, Actuator |
| `backend/src/main/resources/application-dev.yml` | Dev profile (e.g. JPA tuning) |
| `backend/src/main/resources/application-test.yml` | Tests: H2, AI disabled |
| `frontend/proxy.conf.json` | Dev proxy to backend |
| `docker-compose.yml` | Postgres, Prometheus, Grafana |

---

## Testing strategy

- **`@SpringBootTest`** with `test` profile: H2 in-memory DB; Spring AI auto-configurations excluded or disabled; **`ErrorAnalyzerService`** mocked where needed so the context loads without Ollama or Postgres.

---

## Further reading

- Backend: `backend/README.md`
- Frontend: `frontend/README.md`
