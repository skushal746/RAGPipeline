package com.enterprise.ragpipeline.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private static final String NO_CONTEXT_REPLY =
            "I don't have enough context in the knowledge base to answer that question.";

    private final EmbeddingService embeddingService;
    private final MilvusSearchService milvusSearchService;
    private final ChatClient chatClient;

    public RagService(EmbeddingService embeddingService,
                      MilvusSearchService milvusSearchService,
                      ChatClient.Builder chatClientBuilder) {
        this.embeddingService = embeddingService;
        this.milvusSearchService = milvusSearchService;
        this.chatClient = chatClientBuilder.build();
    }

    public String answer(String question) {
        log.info("RAG query: [{}]", question);

        List<Float> vector = embeddingService.embed(question);
        List<String> chunks = milvusSearchService.search(vector);

        if (chunks.isEmpty()) {
            return NO_CONTEXT_REPLY;
        }

        String context = String.join("\n\n---\n\n", chunks);

        String systemPrompt = """
                You are a precise question-answering assistant.
                Answer the user's question using ONLY the context provided below.
                If the answer is not found in the context, respond with "I don't know."
                Do not add information from outside the context.

                Context:
                """ + context;

        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();

        log.info("RAG answer generated for question: [{}]", question);
        return answer;
    }
}
