import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from pydantic import BaseModel

from app.consumer import start_consumer
from app.embedder import Embedder
from app.milvus_store import connect_milvus, ensure_collection

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s %(name)s — %(message)s",
)
log = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("=== ML Worker startup ===")
    log.info("Step 1/3: Loading embedding model...")
    Embedder.get_instance()

    log.info("Step 2/3: Connecting to Milvus and ensuring collection...")
    connect_milvus()
    ensure_collection()

    log.info("Step 3/3: Starting Kafka consumer...")
    await start_consumer()

    log.info("=== ML Worker ready ===")
    yield
    log.info("=== ML Worker shutting down ===")


app = FastAPI(title="RAG ML Worker", version="1.0.0", lifespan=lifespan)


class EmbedRequest(BaseModel):
    prompt: str


class EmbedResponse(BaseModel):
    embedding: list[float]


@app.post("/api/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest) -> EmbedResponse:
    embedder = Embedder.get_instance()
    import asyncio
    vector = await asyncio.to_thread(embedder.embed, request.prompt)
    return EmbedResponse(embedding=vector)


@app.get("/health")
async def health():
    return {"status": "ok"}
