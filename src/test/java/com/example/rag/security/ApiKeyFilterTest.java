package com.example.rag.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;

class ApiKeyFilterTest {

    private final ApiKeyFilter filter = new ApiKeyFilter("secret", new ObjectMapper());

    private MockFilterChain run(ApiKeyFilter f, MockHttpServletRequest request, MockHttpServletResponse response)
            throws ServletException, java.io.IOException {
        MockFilterChain chain = new MockFilterChain();
        f.doFilter(request, response, chain);
        return chain;
    }

    @Test
    void missingKeyIsRejected() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(filter, new MockHttpServletRequest("POST", "/api/ask"), response);

        assertEquals(401, response.getStatus());
        assertNull(chain.getRequest());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
    }

    @Test
    void wrongKeyIsRejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ask");
        request.addHeader("X-API-Key", "nope");
        MockHttpServletResponse response = new MockHttpServletResponse();

        run(filter, request, response);

        assertEquals(401, response.getStatus());
    }

    @Test
    void correctKeyPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ask");
        request.addHeader("X-API-Key", "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(filter, request, response);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void healthEndpointIsOpen() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = run(filter, new MockHttpServletRequest("GET", "/api/health"), response);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void blankConfiguredKeyDisablesTheFilter() throws Exception {
        ApiKeyFilter disabled = new ApiKeyFilter("", new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockFilterChain chain = run(disabled, new MockHttpServletRequest("POST", "/api/ask"), response);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }
}
