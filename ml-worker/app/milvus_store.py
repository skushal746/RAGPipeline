import logging
from pymilvus import (
    Collection,
    CollectionSchema,
    DataType,
    FieldSchema,
    MilvusException,
    connections,
    utility,
)
from app.config import settings

log = logging.getLogger(__name__)

COLLECTION_NAME = "document_chunks"
EMBEDDING_DIM = 384
MAX_TEXT_LEN = 65000


def connect_milvus() -> None:
    connections.connect(alias="default", host=settings.milvus_host, port=settings.milvus_port)
    log.info("Connected to Milvus at %s:%d", settings.milvus_host, settings.milvus_port)


def ensure_collection() -> Collection:
    if utility.has_collection(COLLECTION_NAME):
        log.info("Collection '%s' already exists", COLLECTION_NAME)
        return Collection(COLLECTION_NAME)

    fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        FieldSchema(name="document_id", dtype=DataType.VARCHAR, max_length=64),
        FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=65535),
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=EMBEDDING_DIM),
    ]
    schema = CollectionSchema(fields=fields, description="RAG pipeline document chunks")
    collection = Collection(name=COLLECTION_NAME, schema=schema)

    collection.create_index(
        field_name="embedding",
        index_params={"metric_type": "COSINE", "index_type": "IVF_FLAT", "params": {"nlist": 128}},
    )
    log.info("Created collection '%s' with COSINE/IVF_FLAT index", COLLECTION_NAME)
    return collection


def insert_chunks(document_id: str, texts: list[str], embeddings: list[list[float]]) -> None:
    collection = Collection(COLLECTION_NAME)
    safe_texts = [t[:MAX_TEXT_LEN] for t in texts]
    collection.insert([
        [document_id] * len(texts),
        safe_texts,
        embeddings,
    ])
    collection.flush()
    log.info("Inserted %d chunks for document '%s'", len(texts), document_id)
