package com.enterprise.ragpipeline.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.SearchResults;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MilvusSearchService {

    private static final Logger log = LoggerFactory.getLogger(MilvusSearchService.class);

    private static final String COLLECTION_NAME = "document_chunks";
    private static final String VECTOR_FIELD = "embedding";

    private final MilvusServiceClient milvusClient;

    @Value("${app.milvus.top-k:5}")
    private int topK;

    public MilvusSearchService(MilvusServiceClient milvusClient) {
        this.milvusClient = milvusClient;
    }

    public List<String> search(List<Float> embedding) {
        if (!collectionExists()) {
            log.warn("Collection '{}' does not exist yet — no documents have been processed", COLLECTION_NAME);
            return Collections.emptyList();
        }

        loadCollection();

        SearchParam params = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withMetricType(MetricType.COSINE)
                .withTopK(topK)
                .withVectors(Collections.singletonList(embedding))
                .withVectorFieldName(VECTOR_FIELD)
                .withOutFields(List.of("text"))
                .build();

        R<SearchResults> response = milvusClient.search(params);

        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new IllegalStateException(
                    "Milvus search failed (status=" + response.getStatus() + "): " + response.getMessage()
            );
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
        List<?> texts = wrapper.getFieldWrapper("text").getFieldData();

        List<String> chunks = new ArrayList<>(scores.size());
        for (int i = 0; i < scores.size(); i++) {
            Object text = texts.get(i);
            if (text != null) {
                chunks.add(text.toString());
            }
        }

        log.info("Milvus search returned {} chunks (topK={})", chunks.size(), topK);
        return chunks;
    }

    private boolean collectionExists() {
        R<Boolean> response = milvusClient.hasCollection(
                HasCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build()
        );
        return Boolean.TRUE.equals(response.getData());
    }

    private void loadCollection() {
        milvusClient.loadCollection(
                LoadCollectionParam.newBuilder().withCollectionName(COLLECTION_NAME).build()
        );
    }
}
