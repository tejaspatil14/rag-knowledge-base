package com.example.rag.exception;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.rag.dto.ErrorResponse;

/**
 * Turns every exception into the same clean JSON error.
 * It extends Spring's ResponseEntityExceptionHandler, which already knows the standard
 * web errors (wrong method, missing parameter, unknown URL ...). We only change the body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Used for ResponseStatusException and most standard Spring MVC errors. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        String message = ex.getMessage();

        if (ex instanceof org.springframework.web.server.ResponseStatusException responseStatusException
                && responseStatusException.getReason() != null) {
            message = responseStatusException.getReason();
        }

        if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
            message = problem.getDetail();
        }

        if (statusCode.is5xxServerError()) {
            log.warn("Request failed with {}: {}", statusCode.value(), message);
        }

        return build(statusCode, message, headers);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(status, message.isBlank() ? "Validation failed" : message, headers);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return build(status, "Request body is missing or is not valid JSON", headers);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Object> handleMultipart(MultipartException ex) {
        if (ex instanceof MaxUploadSizeExceededException) {
            return build(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large (limit is 50 MB)", new HttpHeaders());
        }
        return build(HttpStatus.BAD_REQUEST, "Invalid file upload", new HttpHeaders());
    }

    /** Safety net: never show stack traces or internal details to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", new HttpHeaders());
    }

    private ResponseEntity<Object> build(HttpStatusCode status, String message, HttpHeaders headers) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : "Error";
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(status.value(), reason, message));
    }
}
