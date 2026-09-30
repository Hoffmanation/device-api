package com.devices.api.controller;

import com.devices.domain.exception.DeviceDeletionNotAllowedException;
import com.devices.domain.exception.DeviceNotFoundException;
import com.devices.domain.exception.DeviceUpdateNotAllowedException;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ApiExceptionHandler extends RequestBodyAdviceAdapter {

    private static final String CACHED_REQUEST_DTO = "requestDto";
    private final HttpServletRequest request;

    public ApiExceptionHandler(HttpServletRequest request) {
        this.request = request;
    }

    @NonNull
    @Override
    public Object afterBodyRead(@NonNull Object body,
                                @NonNull HttpInputMessage inputMessage,
                                @NonNull MethodParameter parameter,
                                @NonNull Type targetType,
                                @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        request.setAttribute(CACHED_REQUEST_DTO, body);
        return body;
    }

    @Override
    public boolean supports(@NonNull MethodParameter methodParameter,
                            @NonNull Type targetType,
                            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDeviceNotFound(DeviceNotFoundException exception) {
        var status = HttpStatus.NOT_FOUND;
        var errorMessage = exception.getMessage();
        logErrorContext(errorMessage, buildErrorContextForStructualLog(status), exception);
        var errorResponse = ErrorResponse.of(status, "DEVICE_NOT_FOUND", errorMessage, null, null);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler({DeviceUpdateNotAllowedException.class, DeviceDeletionNotAllowedException.class})
    public ResponseEntity<ErrorResponse> handleBusinessConflict(RuntimeException exception) {
        var status = HttpStatus.CONFLICT;
        var errorMessage = exception.getMessage();
        logErrorContext(errorMessage, buildErrorContextForStructualLog(status), exception);
        var errorResponse = ErrorResponse.of(status, "DEVICE_CONFLICT", errorMessage, null, null);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        var status = HttpStatus.BAD_REQUEST;
        List<String> details = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .toList();
        var errorMessage = "Request validation failed";
        logErrorContext(errorMessage + " " + details, buildErrorContextForStructualLog(status), exception);
        var errorResponse = ErrorResponse.of(status, "REQUEST_VALIDATION_FAILED", errorMessage, null, details);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        var status = HttpStatus.BAD_REQUEST;
        List<String> details = exception.getConstraintViolations()
            .stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .toList();
        var errorMessage = "Request validation failed";
        logErrorContext(errorMessage + " " + details, buildErrorContextForStructualLog(status), exception);
        var errorResponse = ErrorResponse.of(status, "REQUEST_VALIDATION_FAILED", errorMessage, null, details);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException exception) {
        var status = HttpStatus.BAD_REQUEST;
        var errorMessage = "Invalid request body";
        var errorCode = isInvalidDeviceState(exception) ? "INVALID_DEVICE_STATE" : "REQUEST_BODY_NOT_READABLE";
        var details = isInvalidDeviceState(exception) ? "state must be one of AVAILABLE, INACTIVE, IN_USE" : null;

        logErrorContext(errorMessage + (details != null ? " " + details : ""), buildErrorContextForStructualLog(status), exception);
        var errorResponse = ErrorResponse.of(status, errorCode, errorMessage, null, details);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        var errorMessage = "An unexpected error occurred";
        logErrorContext(errorMessage, buildErrorContextForStructualLog(status), exception);
        var errorResponse = ErrorResponse.of(status, "INTERNAL_SERVER_ERROR", errorMessage, null, null);
        return ResponseEntity.status(status).body(errorResponse);
    }

    private void logErrorContext(String message, ErrorContext context, Exception exception) {
        log.atError()
            .setMessage("API Exception occurred: " + message)
            .setCause(exception)
            .addKeyValue("error_context", context)
            .log();
    }

    private boolean isInvalidDeviceState(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String typeName = current.getClass().getName();
            String message = current.getMessage();
            if (typeName.contains("InvalidFormatException")
                && message != null
                && message.contains("model.domain.com.devices.DeviceState")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ErrorContext buildErrorContextForStructualLog(HttpStatus status) {
        var endpoint = request.getRequestURI();
        var queryString = request.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            endpoint = endpoint + "?" + queryString;
        }

        var requestBody = request.getAttribute(CACHED_REQUEST_DTO);
        var httpMethod = request.getMethod();

        return new ErrorContext(
            status,
            endpoint,
            requestBody,
            httpMethod
        );
    }

    /**
     * Record representing the context of an API error for structured logging.
     *
     * @param httpStatusCode The HTTP status code of the error response
     * @param endpoint       The full endpoint path including query parameters
     * @param requestBody    The request body object (if available)
     * @param httpMethod     The HTTP method used (GET, POST, PUT, DELETE, etc.)
     */
    public record ErrorContext(
        HttpStatus httpStatusCode,
        String endpoint,
        Object requestBody,
        String httpMethod
    ) {

    }

    /**
     * Record representing a specific validation or business logic failure.
     * Used to provide detailed feedback to the API client about which attribute
     * failed and why.
     *
     * @param code      An error code for the client (e.g., "CONTROLLER_NOT_FOUND")
     * @param message   A description of the error
     * @param attribute The name of the field or property that caused the failure
     * @param value     The invalid value that was provided or a list of validation details
     * @param status    The HTTP status error
     * @param timestamp The timestamp of when the error occurred
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(
        String code,
        String message,
        String error,
        String attribute,
        Object value,
        int status,
        Instant timestamp
    ) {

        public static ErrorResponse of(HttpStatus status, String code, String message, String attribute, Object value) {
            return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(Instant.now())
                .code(code)
                .value(value)
                .attribute(attribute)
                .build();
        }

    }


}
