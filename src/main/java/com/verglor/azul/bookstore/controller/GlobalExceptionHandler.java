package com.verglor.azul.bookstore.controller;

import com.verglor.azul.bookstore.exception.BadRequestException;
import com.verglor.azul.bookstore.exception.ConflictException;
import com.verglor.azul.bookstore.exception.NotFoundException;
import com.verglor.azul.bookstore.openapi.model.ErrorResponse;
import com.verglor.azul.bookstore.openapi.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for centralized exception management across all controllers
 * Provides consistent error responses and appropriate logging for different exception types
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        log.warn("Validation error on request to {}: {}", request.getRequestURI(), ex.getMessage());

        List<ValidationError> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        return renderValidationError(HttpStatus.BAD_REQUEST, "Validation failed for request: " + ex.getMessage(),validationErrors);
    }

    /**
     * Handle bind exceptions (form data binding errors)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindExceptions(
            BindException ex, HttpServletRequest request) {

        log.warn("Bind error on request to {}: {}", request.getRequestURI(), ex.getMessage());

        List<ValidationError> validationErrors = ex.getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .collect(Collectors.toList());

        return renderValidationError(HttpStatus.BAD_REQUEST, "Data binding failed: " + ex.getMessage(), validationErrors);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Illegal argument error on request to {}: {}", request.getRequestURI(), ex.getMessage());

        return renderError(HttpStatus.BAD_REQUEST, "Invalid request parameter: " + ex.getMessage());
    }

    /**
     * Handle method argument type mismatch (e.g., string passed where number expected)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        log.warn("Method argument type mismatch on request to {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());

        return renderError(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        log.warn("Missing request parameter on request to {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("Required parameter '%s' of type %s is missing",
                ex.getParameterName(), ex.getParameterType());

        return renderError(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handle HTTP method not supported
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("HTTP method not supported on request to {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedMethods());

        return renderError(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    /**
     * Handle unsupported media type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        log.warn("Media type not supported on request to {}: {}", request.getRequestURI(), ex.getMessage());

        String message = String.format("Media type '%s' is not supported. Supported media types: %s",
                ex.getContentType(), ex.getSupportedMediaTypes());

        return renderError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }

    /**
     * Handle malformed JSON requests
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed JSON request to {}: {}", request.getRequestURI(), ex.getMessage());

        return renderError(HttpStatus.BAD_REQUEST, "Malformed JSON in request body: " + ex.getMessage());
    }

    /**
     * Handle data integrity violations (e.g., unique constraint violations)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("Data integrity violation on request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return renderError(HttpStatus.CONFLICT, "Data integrity constraint violation: " + ex.getMessage());
    }

    /**
     * Handle 404 - No handler found (for unmatched URLs)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        log.warn("No handler found for request to {}: {}", request.getRequestURI(), ex.getMessage());

        return renderError(HttpStatus.NOT_FOUND, "The requested resource was not found: " + ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("No resource found for request to {}: {}", request.getRequestURI(), ex.getMessage());

        return renderError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {
        return renderError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            NotFoundException ex, HttpServletRequest request) {
        return renderError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex, HttpServletRequest request) {
        return renderError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Internal server error on request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return renderError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    /**
     * Map Spring's FieldError to OpenAPI-generated ValidationError DTO
     */
    private ValidationError mapFieldError(FieldError fieldError) {
        return ValidationError.builder()
                .field(fieldError.getField())
                .message(fieldError.getDefaultMessage())
                .rejectedValue(fieldError.getRejectedValue())
                .build();
    }
    private ResponseEntity<ErrorResponse> renderValidationError(HttpStatus status, String message, List<ValidationError> validationErrors) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(OffsetDateTime.now())
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

    private ResponseEntity<ErrorResponse> renderError(HttpStatus status, String message) {
        return renderValidationError(status, message, new ArrayList<>());
    }

}