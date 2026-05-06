package com.enterprise.ragpipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka message published to the document-processing-queue.
 * Carries only lightweight metadata — the actual file stays in S3 (Claim Check pattern).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIngestionEvent {

    private String documentId;
    private String s3Url;
    private String s3Bucket;
    private String s3Key;
    private String originalFilename;
    private String contentType;
    private long fileSize;
    private Instant uploadedAt;
}
