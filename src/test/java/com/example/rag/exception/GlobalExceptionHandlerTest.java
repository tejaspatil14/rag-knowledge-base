package com.example.rag.exception;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.example.rag.controller.AskController;
import com.example.rag.service.AnswerService;

class GlobalExceptionHandlerTest {

    private final AnswerService answerService = mock(AnswerService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AskController(answerService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void blankQuestionGivesCleanBadRequest() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("question")));
    }

    @Test
    void brokenJsonGivesCleanBadRequest() throws Exception {
        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void responseStatusExceptionKeepsItsStatusAndMessage() throws Exception {
        when(answerService.ask(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama is down"));

        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"hi\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Ollama is down"));
    }

    @Test
    void unexpectedErrorsDoNotLeakDetails() throws Exception {
        when(answerService.ask(anyString())).thenThrow(new RuntimeException("secret internal detail"));

        mvc.perform(post("/api/ask").contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"hi\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}
