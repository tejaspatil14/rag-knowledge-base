package com.example.rag.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rag.dto.SearchRequest;
import com.example.rag.dto.SearchResponse;
import com.example.rag.dto.SearchResult;
import com.example.rag.service.RetrievalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final RetrievalService retrievalService;

    public SearchController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        List<SearchResult> results =
                retrievalService.search(request.query(), request.topK(), request.minScore());
        return new SearchResponse(request.query(), results.size(), results);
    }
}
