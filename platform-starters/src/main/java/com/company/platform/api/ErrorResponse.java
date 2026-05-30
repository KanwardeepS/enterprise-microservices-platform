package com.company.platform.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard error response structure for all API errors.
 */
@Schema(description = "Standard error response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @Schema(description = "Error details")
    private ErrorDetail error;
    
    public ErrorResponse(ErrorDetail error) {
        this.error = error;
    }
    
    public static ErrorResponse of(String code, String message, String path) {
        ErrorDetail detail = new ErrorDetail();
        detail.setCode(code);
        detail.setMessage(message);
        detail.setPath(path);
        detail.setCorrelationId(MDC.get("correlationId"));
        detail.setTimestamp(Instant.now());
        return new ErrorResponse(detail);
    }
    
    public static ErrorResponse of(String code, String message, String path, List<ValidationError> validationErrors) {
        ErrorDetail detail = new ErrorDetail();
        detail.setCode(code);
        detail.setMessage(message);
        detail.setPath(path);
        detail.setDetails(validationErrors);
        detail.setCorrelationId(MDC.get("correlationId"));
        detail.setTimestamp(Instant.now());
        return new ErrorResponse(detail);
    }
    
    // Getters/Setters
    public ErrorDetail getError() {
        return error;
    }
    
    public void setError(ErrorDetail error) {
        this.error = error;
    }
    
    @Schema(description = "Error detail information")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        
        @Schema(description = "Error code", example = "VALIDATION_ERROR")
        private String code;
        
        @Schema(description = "Human-readable error message", example = "Request validation failed")
        private String message;
        
        @Schema(description = "Request path where error occurred", example = "/api/v1/orders")
        private String path;
        
        @Schema(description = "Additional error details")
        private List<?> details;
        
        @Schema(description = "Error timestamp", example = "2026-04-20T10:30:45.123Z")
        private Instant timestamp;
        
        @Schema(description = "Correlation ID for request tracing", example = "550e8400-e29b-41d4-a716-446655440000")
        private String correlationId;
        
        // Getters/Setters
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public List<?> getDetails() {
            return details;
        }
        
        public void setDetails(List<?> details) {
            this.details = details;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
        
        public String getCorrelationId() {
            return correlationId;
        }
        
        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }
    }
    
    @Schema(description = "Validation error detail")
    public static class ValidationError {
        
        @Schema(description = "Field name that failed validation", example = "email")
        private String field;
        
        @Schema(description = "Validation error message", example = "must be a well-formed email address")
        private String message;
        
        @Schema(description = "Rejected value", example = "invalid-email")
        private Object rejectedValue;
        
        public ValidationError() {
        }
        
        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        // Getters/Setters
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Object getRejectedValue() {
            return rejectedValue;
        }
        
        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
    }
}
