package com.example.rag.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.rag.dto.UploadResponse;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.service.IngestionService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;
    private final DocumentChunkRepository repository;

    public DocumentController(IngestionService ingestionService, DocumentChunkRepository repository) {
        this.ingestionService = ingestionService;
        this.repository = repository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingestionService.ingest(file));
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repository.listDocuments();
    }
}
