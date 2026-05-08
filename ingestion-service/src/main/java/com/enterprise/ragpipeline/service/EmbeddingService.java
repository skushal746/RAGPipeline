package com.enterprise.ragpipeline.service;

import com.enterprise.ragpipeline.dto.EmbedRequest;
import com.enterprise.ragpipeline.dto.EmbedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final RestClient mlWorkerRestClient;

    public EmbeddingService(RestClient mlWorkerRestClient) {
        this.mlWorkerRestClient = mlWorkerRestClient;
    }

    public List<Float> embed(String text) {
        log.debug("Requesting embedding for {} chars", text.length());
        try {
            EmbedResponse response = mlWorkerRestClient.post()
                    .uri("/api/embed")
                    .body(new EmbedRequest(text))
                    .retrieve()
                    .body(EmbedResponse.class);

            if (response == null || response.embedding() == null || response.embedding().isEmpty()) {
                throw new IllegalStateException("Empty embedding returned from ml-worker");
            }
            return response.embedding();

        } catch (RestClientException ex) {
            throw new IllegalStateException("ml-worker embed call failed: " + ex.getMessage(), ex);
        }
    }
}
