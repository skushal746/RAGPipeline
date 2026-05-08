package com.enterprise.ragpipeline.controller;

import com.enterprise.ragpipeline.dto.ChatRequest;
import com.enterprise.ragpipeline.dto.ChatResponse;
import com.enterprise.ragpipeline.service.RagService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        log.info("Chat request: question=[{}]", request.question());
        String answer = ragService.answer(request.question());
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
