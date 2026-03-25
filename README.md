# AI Error Analyzer

Full-stack app that analyzes error logs using **RAG** (retrieval-augmented generation): it searches a **PostgreSQL + pgvector** knowledge base, then asks a **local LLM** (via [Ollama](https://ollama.com)) for a structured explanation. **No cloud API key** is required.

| Layer | Stack |
|--------|--------|
| **UI** | Angular 19 (standalone, signals), dev proxy to the API |
| **API** | Spring Boot 3.4, Spring AI, Resilience4j, Actuator + Prometheus metrics |
| **Data** | PostgreSQL 16 with pgvector; markdown knowledge base ingested at startup |
| **Ops** | Docker Compose: Postgres, Prometheus, Grafana (optional) |

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for system design and data flow.

## Repository layout

```
.
├── README.md                 # This file
├── ARCHITECTURE.md           # Architecture and ports
├── docker-compose.yml        # Postgres (5433), Prometheus, Grafana
├── prometheus.yml            # Prometheus scrape config (used by Compose)
├── backend/                  # Spring Boot API (Java 21, Maven)
└── frontend/                 # Angular 19 UI
```

## Prerequisites

- **Java 21** and **Maven 3.9+** (backend)
- **Node.js 20+** and **npm** (frontend)
- **[Ollama](https://ollama.com/download)** (chat + embedding models)
- **Docker** (recommended for Postgres + observability; optional if you use a local Postgres with pgvector)

## Quick start

### 1. Ollama

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

Ensure Ollama is running (`curl -s http://localhost:11434/api/tags`).

### 2. Database

From the repo root:

```bash
docker compose up -d
```

Default JDBC URL uses host port **5433** (see `backend/src/main/resources/application.yml`) to avoid clashing with a local Postgres on 5432.

### 3. Backend

```bash
cd backend
mvn spring-boot:run
```

- Health: [http://localhost:8080/api/health](http://localhost:8080/api/health)
- Analyze: `POST http://localhost:8080/api/analyze` with JSON `{"log":"..."}`

### 4. Frontend

```bash
cd frontend
npm install
npm start
```

Open [http://localhost:4200](http://localhost:4200). The dev server proxies `/api` to `http://localhost:8080` (`frontend/proxy.conf.json`).

## Common ports

| Service | Port |
|---------|------|
| Angular dev server | 4200 |
| Spring Boot | 8080 |
| Ollama | 11434 |
| Postgres (Compose) | **5433** → container 5432 |
| Prometheus (Compose) | 9090 |
| Grafana (Compose) | 3000 |

## Troubleshooting

- **`503` on `/api/analyze` (AI unavailable):** Start Ollama and Postgres first, confirm models with `ollama list`, then **restart** the Spring Boot app. Details: [backend/README.md](backend/README.md).
- **Port 5432 already in use:** Compose maps Postgres to **5433** on the host by design.

## Further reading

- [backend/README.md](backend/README.md) — Spring config, profiles, package layout
- [frontend/README.md](frontend/README.md) — Angular CLI commands
- [ARCHITECTURE.md](ARCHITECTURE.md) — components, data flow, observability

## License

Add a license file if you distribute this project publicly.
