package com.enterprise.ragpipeline.controller;

import com.enterprise.ragpipeline.dto.DocumentUploadResponse;
import com.enterprise.ragpipeline.service.DocumentService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
@Validated
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> upload(
            @RequestPart("file") @NotNull MultipartFile file) {

        log.info("Upload request received: filename=[{}], size=[{} bytes], contentType=[{}]",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        DocumentUploadResponse response = documentService.ingestDocument(file);

        log.info("Ingestion initiated: documentId=[{}]", response.getDocumentId());

        // 202 Accepted — file is queued; processing is async downstream
        return ResponseEntity.accepted().body(response);
    }
}
