package com.oms.iam.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();

        if ("Email already exists".equals(message)) {
            status = HttpStatus.CONFLICT;
        } else if ("Invalid credentials".equals(message)) {
            status = HttpStatus.UNAUTHORIZED;
        } else if ("User is not active".equals(message)) {
            status = HttpStatus.FORBIDDEN;
        }

        ErrorResponse error = new ErrorResponse(status.value(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(error);
    }

    public record ErrorResponse(int status, String message, LocalDateTime timestamp) {
    }
}
