package com.enterprise.ragpipeline.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
public class DocumentUploadResponse {

    private String documentId;
    private String s3Url;
    private String originalFilename;
    private long fileSize;
    private Instant queuedAt;
    /** QUEUED — uploaded to S3 and event published to Kafka. */
    private String status;
}
