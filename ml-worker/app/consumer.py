import asyncio
import json
import logging

from aiokafka import AIOKafkaConsumer

from app.config import settings
from app.embedder import Embedder
from app.milvus_store import insert_chunks
from app.processor import chunk_text, download_from_s3, extract_text

log = logging.getLogger(__name__)


async def start_consumer() -> None:
    asyncio.create_task(_consume_loop())


async def _consume_loop() -> None:
    consumer = AIOKafkaConsumer(
        settings.kafka_topic,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_group_id,
        value_deserializer=lambda raw: json.loads(raw.decode("utf-8")),
        auto_offset_reset="earliest",
        enable_auto_commit=True,
    )
    await consumer.start()
    log.info("Kafka consumer started — topic='%s' group='%s'", settings.kafka_topic, settings.kafka_group_id)
    try:
        async for msg in consumer:
            await _handle(msg.value)
    except Exception:
        log.exception("Kafka consumer loop crashed")
    finally:
        await consumer.stop()
        log.info("Kafka consumer stopped")


async def _handle(event: dict) -> None:
    document_id = event.get("documentId", "unknown")
    s3_bucket = event["s3Bucket"]
    s3_key = event["s3Key"]
    content_type = event.get("contentType", "text/plain")

    log.info("Received event: documentId=%s bucket=%s key=%s", document_id, s3_bucket, s3_key)
    try:
        content = await asyncio.to_thread(download_from_s3, s3_bucket, s3_key)
        text = extract_text(content, content_type)

        if not text.strip():
            log.warning("Document %s produced no extractable text — skipping", document_id)
            return

        chunks = chunk_text(text)
        log.info("Document %s: %d chunks", document_id, len(chunks))

        embedder = Embedder.get_instance()
        embeddings = await asyncio.to_thread(embedder.embed_batch, chunks)

        await asyncio.to_thread(insert_chunks, document_id, chunks, embeddings)
        log.info("Document %s fully processed and stored in Milvus", document_id)

    except Exception:
        log.exception("Failed to process document %s — skipping", document_id)
