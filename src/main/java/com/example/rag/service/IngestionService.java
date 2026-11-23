package com.example.rag.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.rag.dto.UploadResponse;
import com.example.rag.repository.DocumentChunkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "txt");

    private final DocumentParser parser;
    private final ChunkingService chunker;
    private final DocumentChunkRepository repository;
    private final ObjectMapper objectMapper;

    public IngestionService(DocumentParser parser, ChunkingService chunker,
                            DocumentChunkRepository repository, ObjectMapper objectMapper) {
        this.parser = parser;
        this.chunker = chunker;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UploadResponse ingest(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        String source = StringUtils.getFilename(StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
        if (source == null || source.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File has no name");
        }
        String extension = StringUtils.getFilenameExtension(source);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported file type. Allowed: " + ALLOWED_EXTENSIONS);
        }

        // 1. Extract text
        String text;
        try (InputStream in = file.getInputStream()) {
            text = parser.extractText(in);
        } catch (IOException | TikaException e) {
            log.error("Could not read {}", source, e);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not extract text from " + source);
        }
        log.info("Extracted {} characters from {}", text.length(), source);

        // 2. Chunk
        List<String> chunks = chunker.chunk(text);
        if (chunks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No text found in " + source + " (is it a scanned PDF?)");
        }
        log.info("Created {} chunks from {}", chunks.size(), source);

        // 3. Save. Uploading the same file name again replaces the old chunks.
        int removed = repository.deleteBySource(source);
        if (removed > 0) {
            log.info("Replaced {} old chunks of {}", removed, source);
        }
        repository.saveChunks(source, chunks, buildMetadata(source, file.getContentType()));

        return new UploadResponse(source, chunks.size(), text.length(), "Document ingested successfully");
    }

    private String buildMetadata(String fileName, String contentType) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "fileName", fileName,
                    "contentType", contentType == null ? "unknown" : contentType,
                    "ingestedAt", Instant.now().toString()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not build metadata", e);
        }
    }
}
