package com.example.rag.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Integer db = jdbc.queryForObject("SELECT 1", Integer.class);
        Boolean pgvector = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')", Boolean.class);
        return Map.of(
                "status", "OK",
                "database", db != null && db == 1 ? "connected" : "error",
                "pgvector", Boolean.TRUE.equals(pgvector) ? "enabled" : "missing");
    }
}
