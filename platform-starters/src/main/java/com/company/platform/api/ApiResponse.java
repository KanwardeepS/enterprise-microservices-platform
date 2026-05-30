package com.company.platform.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * Standard API response wrapper providing consistent structure across all endpoints.
 * 
 * @param <T> Type of the response data
 */
@Schema(description = "Standard API response wrapper")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    @Schema(description = "Response data")
    private T data;
    
    @Schema(description = "Response metadata")
    private ResponseMetadata metadata;
    
    private ApiResponse(T data, ResponseMetadata metadata) {
        this.data = data;
        this.metadata = metadata;
    }
    
    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, createMetadata(null));
    }
    
    /**
     * Create a successful response with data and pagination metadata
     */
    public static <T> ApiResponse<T> success(T data, PaginationMetadata pagination) {
        return new ApiResponse<>(data, createMetadata(pagination));
    }
    
    private static ResponseMetadata createMetadata(PaginationMetadata pagination) {
        ResponseMetadata metadata = new ResponseMetadata();
        metadata.setCorrelationId(MDC.get("correlationId"));
        metadata.setTimestamp(Instant.now());
        metadata.setPagination(pagination);
        return metadata;
    }
    
    // Getters/Setters
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public ResponseMetadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(ResponseMetadata metadata) {
        this.metadata = metadata;
    }
    
    @Schema(description = "Response metadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResponseMetadata {
        
        @Schema(description = "Request correlation ID for tracing", example = "550e8400-e29b-41d4-a716-446655440000")
        private String correlationId;
        
        @Schema(description = "Response generation timestamp", example = "2026-04-20T10:30:45.123Z")
        private Instant timestamp;
        
        @Schema(description = "Pagination information (only for collection responses)")
        private PaginationMetadata pagination;
        
        // Getters/Setters
        public String getCorrelationId() {
            return correlationId;
        }
        
        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
        
        public PaginationMetadata getPagination() {
            return pagination;
        }
        
        public void setPagination(PaginationMetadata pagination) {
            this.pagination = pagination;
        }
    }
}
