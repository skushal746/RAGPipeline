# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Run the full stack
```bash
docker compose up          # foreground; first run downloads Maven deps and the ML embedding model
docker compose up -d       # background
docker compose down        # stop, keep volumes
docker compose down -v     # stop and wipe all data
```

### Watch logs for a service
```bash
docker compose logs -f ingestion-service
docker compose logs -f ml-worker
```

### Ingestion service tests (requires Java 17+)
```bash
cd ingestion-service
./mvnw test                          # all tests
./mvnw test -Dtest=ClassName         # single test class
./mvnw test -Dtest=ClassName#method  # single test method
```

### Rebuild ML worker after source changes (no hot reload)
```bash
docker compose build ml-worker && docker compose up -d ml-worker
```

### Check stack readiness
```bash
until curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; do
  echo "Waiting..."; sleep 5
done
```

## Environment setup

Create `.env` in the project root:
```
OPENAI_API_KEY=sk-...
JWT_SECRET=your-secret-key-at-least-32-characters-long  # optional override
```

## Architecture

```
React SPA (3000)
    │ REST (JWT Bearer)
    ▼
Ingestion Service — Spring Boot 3.3 / Java 17 (8080)
    │ S3 PUT          │ Kafka event (document-processing-queue)
    ▼                 ▼
LocalStack/S3      ML Worker — FastAPI / Python 3.13 (8000)
(4566)                 │ pymilvus insert
                       ▼
                  Milvus (19530 gRPC / 9091 HTTP)
                  — etcd (metadata) + MinIO (index storage)
```

### Ingestion Service (`ingestion-service/`)
Spring Boot application with three controller layers:
- `AuthController` — register/login, returns JWT
- `DocumentController` — multipart upload (≤50 MB) → S3 → Kafka event
- `ChatController` — RAG query endpoint (Phase 3)

Key services:
- `DocumentService` — uploads to S3 via Spring Cloud AWS, publishes `DocumentIngestionEvent` to Kafka
- `KafkaProducerService` — publishes JSON events; topic name sourced from `KafkaTopicProperties` record (`app.kafka.topics.document-processing`)
- `RagService` — calls `EmbeddingService` (HTTP to ml-worker `/api/embed`) → `MilvusSearchService` → Spring AI `ChatClient` (GPT-4o-mini)
- `JwtService` / `JwtAuthFilter` — HMAC-SHA256 JWT; 24-hour expiry; secret from `app.jwt.secret`

Security: all endpoints except `/api/auth/**` require a valid JWT. `SecurityConfig` configures stateless session management.

JPA: `User` entity / `UserRepository` backed by PostgreSQL (`ragdb`). Schema managed by Hibernate `ddl-auto: update`.

### ML Worker (`ml-worker/`)
FastAPI app with an async lifespan that runs three startup steps:
1. Load `all-MiniLM-L6-v2` embedding model (singleton `Embedder`)
2. Connect to Milvus and ensure `document_chunks` collection (384-dim COSINE/IVF_FLAT index)
3. Start `aiokafka` consumer (`ml-worker-group`) on `document-processing-queue`

Processing pipeline per Kafka message: download from S3 → extract text (PDF via PyMuPDF, DOCX via python-docx, plain text fallback) → chunk with LangChain `RecursiveCharacterTextSplitter` (1000 chars / 200 overlap) → embed batch → insert into Milvus.

Exposes `POST /api/embed` for on-demand embedding (called by the ingestion service's `EmbeddingService`).

### Frontend (`frontend/`)
React 18 SPA built with Vite + TypeScript. Two authenticated routes:
- `/upload` — document upload form → `POST /api/documents/upload`
- `/chat` — RAG chat interface → `POST /api/chat`

Auth state managed via `AuthContext`; `ProtectedRoute` guards authenticated pages. Vite dev server proxies API calls to `http://ingestion-service:8080` via `API_TARGET` env var.

### Infrastructure
- **Kafka**: KRaft mode (no Zookeeper). Internal listener `kafka:29092` for containers; external `localhost:9092` for host.
- **LocalStack**: S3-only emulation. Bucket `rag-pipeline-documents` auto-created by `init-localstack.sh` on first startup.
- **Milvus**: Requires both etcd and MinIO (separate from LocalStack) to be healthy before starting. GUI at Attu (8001).
- **PostgreSQL**: `raguser`/`ragpass`/`ragdb`

## Phase status
| Phase | Status | What it does |
|-------|--------|--------------|
| 1 | Complete | Upload → S3 → Kafka event |
| 2 | Complete | ML Worker: consume → chunk → embed → Milvus |
| 3 | Pending | RAG query: Milvus search + GPT-4o-mini response |
