package com.enterprise.ragpipeline.service;

import com.enterprise.ragpipeline.config.S3Properties;
import com.enterprise.ragpipeline.dto.DocumentIngestionEvent;
import com.enterprise.ragpipeline.dto.DocumentUploadResponse;
import com.enterprise.ragpipeline.exception.DocumentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final KafkaProducerService kafkaProducerService;

    public DocumentService(S3Client s3Client,
                           S3Properties s3Properties,
                           KafkaProducerService kafkaProducerService) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.kafkaProducerService = kafkaProducerService;
    }

    public DocumentUploadResponse ingestDocument(MultipartFile file) {
        validateFile(file);

        String documentId = UUID.randomUUID().toString();
        String s3Key = buildS3Key(documentId, file.getOriginalFilename());
        Instant now = Instant.now();

        log.info("Uploading document [{}] → s3://{}/{}", documentId, s3Properties.getBucketName(), s3Key);

        String s3Url = uploadToS3(file, s3Key);

        DocumentIngestionEvent event = DocumentIngestionEvent.builder()
                .documentId(documentId)
                .s3Url(s3Url)
                .s3Bucket(s3Properties.getBucketName())
                .s3Key(s3Key)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(now)
                .build();

        kafkaProducerService.publishIngestionEvent(event);

        return DocumentUploadResponse.builder()
                .documentId(documentId)
                .s3Url(s3Url)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .queuedAt(now)
                .status("QUEUED")
                .build();
    }

    private String uploadToS3(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(Map.of(
                            "original-filename", sanitizeHeader(file.getOriginalFilename()),
                            "upload-timestamp", Instant.now().toString()
                    ))
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return String.format("s3://%s/%s", s3Properties.getBucketName(), s3Key);

        } catch (IOException e) {
            throw new DocumentStorageException("Failed to read uploaded file for S3 transfer", e);
        } catch (S3Exception e) {
            throw new DocumentStorageException(
                    String.format("S3 upload failed for key [%s]: %s", s3Key, e.getMessage()), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CONTENT_TYPES.contains(ct)) {
            throw new IllegalArgumentException(
                    String.format("Unsupported content type [%s]. Accepted: %s", ct, ALLOWED_CONTENT_TYPES));
        }
    }

    private String buildS3Key(String documentId, String originalFilename) {
        String safe = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "unknown";
        return s3Properties.getKeyPrefix() + documentId + "/" + safe;
    }

    /** Strips non-ASCII characters that are illegal in S3 object metadata headers. */
    private String sanitizeHeader(String value) {
        return Objects.requireNonNullElse(value, "unknown")
                .replaceAll("[^ -~]", "_");
    }
}
