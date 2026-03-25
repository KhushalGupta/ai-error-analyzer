# AI Error Analyzer - Backend

Spring Boot 3.4 backend with Spring AI, Ollama (local LLM), PGVector, and JPA. **No API key required** — runs entirely locally.

## Requirements

- Java 21
- Maven 3.9+
- [Ollama](https://ollama.com/download) (local AI — free, no API key)
- PostgreSQL with PGVector extension (Docker Compose provided)

## Quick Start

1. **Install and run Ollama**, then pull the models:
   ```bash
   ollama pull llama3.2
   ollama pull nomic-embed-text
   ```

2. **Start PostgreSQL with PGVector** (Docker):
   ```bash
   docker compose up -d
   ```

3. **Build and run:**
   ```bash
   cd backend && mvn spring-boot:run
   ```

4. **Health check:** `GET http://localhost:8080/api/health`

### If `/api/analyze` returns 503 (AI service unavailable)

1. Start **Ollama** before or with the backend (`http://localhost:11434` must respond).
2. Pull models: `ollama pull llama3.2` and `ollama pull nomic-embed-text` (or `ollama list` to verify names).
3. Ensure **PostgreSQL** (with pgvector) is reachable on the JDBC URL in `application.yml` (`spring.datasource.url`).
4. **Restart** Spring Boot after Ollama is up so `ChatModel` and `VectorStore` beans register.

The app registers `ErrorAnalyzerService` only when both `VectorStore` and `ChatModel` are available; `spring.ai.ollama.init.pull-model-strategy: when_missing` can download missing models on startup (may take a while the first time).

## Configuration

- `application.yml` - Main config (PostgreSQL, Spring AI, JPA)
- `application-dev.yml` - Dev profile (schema update, debug logging)
- `application-test.yml` - Test profile (H2 in-memory, AI disabled)

## Package Structure

```
com.ai.error_analyzer
├── ErrorAnalyzerApplication.java
├── controller/     # REST endpoints
├── service/        # Business logic
└── repository/     # JPA repositories
```
