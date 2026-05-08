from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_topic: str = "document-processing-queue"
    kafka_group_id: str = "ml-worker-group"

    aws_s3_endpoint_url: str = "http://localhost:4566"
    aws_access_key_id: str = "test"
    aws_secret_access_key: str = "test"
    aws_default_region: str = "us-east-1"

    milvus_host: str = "localhost"
    milvus_port: int = 19530

    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
