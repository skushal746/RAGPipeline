package com.enterprise.ragpipeline.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicProperties {

    @NotBlank
    private String documentProcessing;

    public String getDocumentProcessing() {
        return documentProcessing;
    }

    public void setDocumentProcessing(String documentProcessing) {
        this.documentProcessing = documentProcessing;
    }
}
