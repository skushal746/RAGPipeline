import logging
from sentence_transformers import SentenceTransformer

log = logging.getLogger(__name__)

MODEL_NAME = "all-MiniLM-L6-v2"
EMBEDDING_DIM = 384


class Embedder:
    _instance: "Embedder | None" = None

    def __init__(self) -> None:
        log.info("Loading sentence-transformer model '%s'", MODEL_NAME)
        self._model = SentenceTransformer(MODEL_NAME)
        log.info("Model loaded (dim=%d)", EMBEDDING_DIM)

    @classmethod
    def get_instance(cls) -> "Embedder":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def embed(self, text: str) -> list[float]:
        return self._model.encode(text, normalize_embeddings=True).tolist()

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        return self._model.encode(texts, normalize_embeddings=True, batch_size=32).tolist()
