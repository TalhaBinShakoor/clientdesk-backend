package com.clientdesk.error;

import com.clientdesk.api.RequestBodyTooLargeException;
import com.clientdesk.security.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE = "An unexpected error occurred";
    private final SecurityAuditLogger securityAuditLogger;

    public ApiExceptionHandler(SecurityAuditLogger securityAuditLogger) {
        this.securityAuditLogger = securityAuditLogger;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode status = exception.getStatusCode();
        String message = status.is5xxServerError()
                ? INTERNAL_ERROR_MESSAGE
                : safeMessage(exception.getReason(), "Request could not be completed");

        if (status.is5xxServerError()) {
            securityAuditLogger.apiFailure(
                    status.value(),
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getClass()
            );
        }

        return response(status, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return response(HttpStatus.BAD_REQUEST, "Request validation failed", request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            MissingServletRequestPartException.class,
            MultipartException.class
    })
    ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        if (hasCause(exception, RequestBodyTooLargeException.class)) {
            return response(HttpStatus.PAYLOAD_TOO_LARGE, "JSON request body is too large", request);
        }
        return response(HttpStatus.BAD_REQUEST, "Request is malformed", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        securityAuditLogger.apiFailure(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass()
        );
        return response(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MESSAGE, request);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatusCode status,
            String message,
            HttpServletRequest request
    ) {
        HttpStatus resolvedStatus = HttpStatus.resolve(status.value());
        String error = resolvedStatus == null ? "Request failed" : resolvedStatus.getReasonPhrase();
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    private String safeMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
