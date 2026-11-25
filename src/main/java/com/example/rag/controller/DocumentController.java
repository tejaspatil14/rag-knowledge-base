package com.example.rag.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.rag.dto.EmbeddingRunResponse;
import com.example.rag.dto.UploadResponse;
import com.example.rag.repository.DocumentChunkRepository;
import com.example.rag.service.EmbeddingPipelineService;
import com.example.rag.service.IngestionService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;
    private final EmbeddingPipelineService embeddingPipeline;
    private final DocumentChunkRepository repository;

    public DocumentController(IngestionService ingestionService, EmbeddingPipelineService embeddingPipeline,
                              DocumentChunkRepository repository) {
        this.ingestionService = ingestionService;
        this.embeddingPipeline = embeddingPipeline;
        this.repository = repository;
    }

    /** Upload + chunk + (by default) embed, so the document is searchable right after this call. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam(defaultValue = "true") boolean embed) {
        UploadResponse saved = ingestionService.ingest(file);
        if (!embed) {
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        }
        try {
            EmbeddingRunResponse run = embeddingPipeline.embedPendingChunks();
            return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponse(
                    saved.source(), saved.totalChunks(), saved.totalCharacters(),
                    "Document ingested and " + run.embeddedNow() + " chunks embedded"));
        } catch (ResponseStatusException e) {
            throw new ResponseStatusException(e.getStatusCode(),
                    "Document was saved, but embedding failed: " + e.getReason()
                            + " You can retry with POST /api/embeddings/run",
                    e);
        }
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repository.listDocuments();
    }

    /** DELETE /api/documents?source=handbook.pdf */
    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestParam String source) {
        int deleted = repository.deleteBySource(source);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No document named '" + source + "'");
        }
        return ResponseEntity.noContent().build();
    }
}
