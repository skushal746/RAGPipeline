# Enterprise RAG Pipeline

A full-stack Retrieval-Augmented Generation (RAG) pipeline for document ingestion, semantic search, and AI-powered querying.

## Architecture Overview

```
React Frontend (3000)
        │
        ▼
Ingestion Service — Spring Boot (8080)
        │                  │
        │ Kafka events      │ S3 uploads
        ▼                  ▼
   ML Worker (8000)    LocalStack / S3
        │
        ▼
  Milvus Vector DB (19530)
```

**Services**

| Service | Port | Description |
|---|---|---|
| Frontend | 3000 | React SPA (Vite dev server) |
| Ingestion Service | 8080 | Spring Boot REST API |
| ML Worker | 8000 | Python Kafka consumer + embeddings (Phase 2) |
| Kafka | 9092 | KRaft-mode message broker |
| Kafka UI | 8090 | Kafbat dashboard |
| PostgreSQL | 5432 | User auth database |
| LocalStack | 4566 | S3 emulation |
| Milvus | 19530 / 9091 | Vector database (gRPC / HTTP) |
| Attu | 8001 | Milvus GUI |
| MinIO Console | 9001 | Milvus object storage UI |

---

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose v2
- An OpenAI API key (required for the AI query layer)

> No local Java, Node, or Python installation is required — everything runs inside containers.

---

## Setup

### 1. Clone the repository

```bash
git clone <repo-url>
cd RAGPipeline
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```bash
# Required — OpenAI key for the RAG query layer
OPENAI_API_KEY=sk-...

# Optional — override the default JWT secret before deploying anywhere non-local
JWT_SECRET=your-secret-key-at-least-32-characters-long
```

The docker-compose file reads these automatically. Without `OPENAI_API_KEY` the ingestion and frontend services will start, but AI-powered queries will fail.

---

## Starting the Application

### Full stack (recommended)

```bash
docker compose up
```

First run takes several minutes — Maven dependencies and the ML worker's embedding model (`all-MiniLM-L6-v2`) are downloaded and cached. Subsequent starts are fast.

### Detached (background) mode

```bash
docker compose up -d
```

### Watch logs for a specific service

```bash
docker compose logs -f ingestion-service
docker compose logs -f ml-worker
docker compose logs -f frontend
```

### Wait for readiness

The ingestion service has a health check. Poll it to know when the stack is fully up:

```bash
until curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"'; do
  echo "Waiting for ingestion-service..."; sleep 5
done
echo "Stack is ready."
```

---

## Accessing the Application

| URL | What it is |
|---|---|
| http://localhost:3000 | React frontend |
| http://localhost:8080/actuator/health | Ingestion service health |
| http://localhost:8080/swagger-ui.html | API docs (if Swagger is enabled) |
| http://localhost:8090 | Kafka UI (Kafbat) |
| http://localhost:8001 | Attu — Milvus GUI |
| http://localhost:9001 | MinIO Console |

---

## Stopping the Application

```bash
# Stop containers (keeps volumes — data is preserved)
docker compose down

# Stop and delete all data volumes (full reset)
docker compose down -v
```

---

## Development Workflow

The ingestion service and frontend run in live-reload mode inside Docker — no rebuild needed for source changes.

| Component | Hot reload? | How |
|---|---|---|
| Ingestion Service | Yes | `mvn spring-boot:run` inside container picks up changes via volume mount |
| Frontend | Yes | Vite HMR via volume mount |
| ML Worker | No | Rebuild required: `docker compose build ml-worker && docker compose up ml-worker` |

### Rebuild a single service

```bash
docker compose build <service-name>
docker compose up -d <service-name>
```

### Run ingestion service tests locally (requires Java 17+)

```bash
cd ingestion-service
./mvnw test
```

---

## Infrastructure Notes

- **S3 bucket** (`rag-pipeline-documents`) is created automatically by `init-localstack.sh` on first LocalStack startup.
- **Kafka** runs in KRaft mode (no Zookeeper). The `document-processing-queue` topic is auto-created with 3 partitions on ingestion service startup.
- **Milvus** depends on etcd (metadata) and MinIO (index storage) — both start automatically.
- **PostgreSQL** credentials: `raguser` / `ragpass` / database `ragdb`.

---

## Project Phases

| Phase | Status | Description |
|---|---|---|
| Phase 1 | Complete | Document upload → S3 → Kafka event |
| Phase 2 | In progress | ML Worker: Kafka consumer, chunking, embeddings → Milvus |
| Phase 3 | Pending | RAG query layer: semantic search + GPT-4o-mini response |