package com.enterprise.ragpipeline.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(@NotBlank String documentProcessing) {}
