package com.enterprise.ragpipeline.service;

import com.enterprise.ragpipeline.config.KafkaTopicProperties;
import com.enterprise.ragpipeline.dto.DocumentIngestionEvent;
import com.enterprise.ragpipeline.exception.DocumentStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final int SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, DocumentIngestionEvent> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public KafkaProducerService(KafkaTemplate<String, DocumentIngestionEvent> kafkaTemplate,
                                KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    /**
     * Publishes a DocumentIngestionEvent to the document-processing topic.
     * Uses documentId as the partition key so all events for a document land
     * on the same partition (ordering guarantee per document).
     * Blocks for up to SEND_TIMEOUT_SECONDS to confirm broker acknowledgment
     * before returning HTTP 202 to the caller.
     */
    public void publishIngestionEvent(DocumentIngestionEvent event) {
        String topic = topicProperties.getDocumentProcessing();

        // documentId is always set by DocumentService before this call
        String documentId = Objects.requireNonNull(event.getDocumentId(), "documentId must not be null");
        String resolvedTopic = Objects.requireNonNull(topic);

        log.info("Publishing ingestion event: documentId=[{}], topic=[{}]", documentId, resolvedTopic);

        try {
            SendResult<String, DocumentIngestionEvent> result = kafkaTemplate
                    .send(resolvedTopic, documentId, event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log.info("Event published: documentId=[{}], partition=[{}], offset=[{}]",
                    documentId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (ExecutionException e) {
            throw new DocumentStorageException(
                    String.format("Kafka publish failed for document [%s]", documentId),
                    e.getCause());

        } catch (TimeoutException e) {
            throw new DocumentStorageException(
                    String.format("Kafka publish timed out after %ds for document [%s]",
                            SEND_TIMEOUT_SECONDS, documentId), e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentStorageException(
                    String.format("Kafka publish interrupted for document [%s]", documentId), e);
        }
    }
}
