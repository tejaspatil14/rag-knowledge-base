package com.example.rag.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.rag.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Every /api/** request needs the header  X-API-Key: <your key>  (except /api/health).
 * If no key is configured, the filter is switched off (local development only).
 *
 * This is a deliberately small solution. For real users and roles, use Spring Security + JWT/OAuth2.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    static final String HEADER = "X-API-Key";

    private final byte[] expectedKey;
    private final boolean enabled;
    private final ObjectMapper objectMapper;

    public ApiKeyFilter(@Value("${rag.security.api-key:}") String apiKey, ObjectMapper objectMapper) {
        this.expectedKey = apiKey.getBytes(StandardCharsets.UTF_8);
        this.enabled = !apiKey.isBlank();
        this.objectMapper = objectMapper;
        if (!enabled) {
            log.warn("API key authentication is DISABLED. Set RAG_API_KEY before you deploy this anywhere.");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !enabled
                || !path.startsWith("/api/")
                || path.equals("/api/health")
                || "OPTIONS".equalsIgnoreCase(request.getMethod()); // CORS pre-flight from a future frontend
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        // MessageDigest.isEqual compares in constant time, so timing cannot leak the key.
        if (provided != null && MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(401, "Unauthorized",
                "Missing or invalid " + HEADER + " header"));
    }
}
