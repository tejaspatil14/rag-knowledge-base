package com.example.rag.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rag.dto.AskRequest;
import com.example.rag.dto.AskResponse;
import com.example.rag.service.AnswerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AskController {

    private final AnswerService answerService;

    public AskController(AnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return answerService.ask(request.question());
    }
}
