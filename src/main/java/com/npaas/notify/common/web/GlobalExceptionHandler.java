package com.npaas.notify.common.web;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.npaas.notify.templates.NotificationTemplateNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldViolation)
            .toList();

        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.validation("Request validation failed", request.getRequestURI(), violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<FieldViolation> violations = exception.getConstraintViolations()
            .stream()
            .map(this::toFieldViolation)
            .toList();

        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.validation("Request validation failed", request.getRequestURI(), violations));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String reason = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return ResponseEntity.status(status)
            .body(ApiErrorResponse.of(status.value(), status.getReasonPhrase(), reason, request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of(400, "Bad Request", "Invalid request", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.of(409, "Conflict", "Request conflicts with existing data", request.getRequestURI()));
    }

    @ExceptionHandler(NotificationTemplateNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotificationTemplateNotFound(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of(404, "Not Found", "Notification template was not found", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of(500, "Internal Server Error", "Unexpected server error", request.getRequestURI()));
    }

    private FieldViolation toFieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), safeMessage(error.getDefaultMessage()));
    }

    private FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
        return new FieldViolation(violation.getPropertyPath().toString(), safeMessage(violation.getMessage()));
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Invalid value";
        }

        return message;
    }
}
