package com.enterprise.ragpipeline.config;

import com.enterprise.ragpipeline.dto.DocumentIngestionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Typed producer factory for DocumentIngestionEvent.
     * Uses a dedicated ObjectMapper with JavaTimeModule so Instant fields
     * serialize as ISO-8601 strings rather than epoch arrays.
     * Defining this bean causes Spring Boot's generic ProducerFactory to back off.
     */
    @Bean
    public ProducerFactory<String, DocumentIngestionEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30_000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 10_000);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JsonSerializer<>(mapper));
    }

    @Bean
    public KafkaTemplate<String, DocumentIngestionEvent> kafkaTemplate(
            ProducerFactory<String, DocumentIngestionEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Declares the topic on the broker at startup (no-op if it already exists).
     * Partitions=3 allows parallel consumption by up to 3 ML worker instances.
     */
    @Bean
    public NewTopic documentProcessingTopic(KafkaTopicProperties topicProperties) {
        return TopicBuilder.name(topicProperties.documentProcessing())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
