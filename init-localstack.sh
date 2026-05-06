#!/bin/bash
# Runs inside LocalStack on first startup — creates the S3 bucket used by the pipeline.
set -e

echo ">>> Creating S3 bucket: rag-pipeline-documents"
awslocal s3 mb s3://rag-pipeline-documents --region us-east-1
echo ">>> Bucket ready."
