import io
import logging

import boto3
import fitz  # PyMuPDF
from docx import Document as DocxDocument
from langchain_text_splitters import RecursiveCharacterTextSplitter
from app.config import settings

log = logging.getLogger(__name__)

_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)


def download_from_s3(bucket: str, key: str) -> bytes:
    client = boto3.client(
        "s3",
        endpoint_url=settings.aws_s3_endpoint_url,
        region_name=settings.aws_default_region,
        aws_access_key_id=settings.aws_access_key_id,
        aws_secret_access_key=settings.aws_secret_access_key,
    )
    response = client.get_object(Bucket=bucket, Key=key)
    return response["Body"].read()


def extract_text(content: bytes, content_type: str) -> str:
    if content_type == "application/pdf":
        return _extract_pdf(content)
    if content_type in (
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ):
        return _extract_docx(content)
    return content.decode("utf-8", errors="replace")


def chunk_text(text: str) -> list[str]:
    chunks = _splitter.split_text(text)
    non_empty = [c.strip() for c in chunks if c.strip()]
    log.debug("Produced %d chunks from %d characters", len(non_empty), len(text))
    return non_empty


def _extract_pdf(content: bytes) -> str:
    with fitz.open(stream=content, filetype="pdf") as doc:
        pages = [page.get_text() for page in doc]
    return "\n".join(pages)


def _extract_docx(content: bytes) -> str:
    doc = DocxDocument(io.BytesIO(content))
    return "\n".join(p.text for p in doc.paragraphs if p.text.strip())
