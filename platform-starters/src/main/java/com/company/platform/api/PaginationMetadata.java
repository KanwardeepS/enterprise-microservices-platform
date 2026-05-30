package com.company.platform.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * Pagination metadata for collection responses.
 */
@Schema(description = "Pagination information")
public class PaginationMetadata {
    
    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int page;
    
    @Schema(description = "Page size", example = "20")
    private int size;
    
    @Schema(description = "Total number of elements", example = "150")
    private long totalElements;
    
    @Schema(description = "Total number of pages", example = "8")
    private int totalPages;
    
    @Schema(description = "Whether there is a next page", example = "true")
    private boolean hasNext;
    
    @Schema(description = "Whether there is a previous page", example = "false")
    private boolean hasPrevious;
    
    public PaginationMetadata() {
    }
    
    public PaginationMetadata(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }
    
    /**
     * Create pagination metadata from Spring Data Page
     */
    public static PaginationMetadata from(Page<?> page) {
        return new PaginationMetadata(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
    
    // Getters/Setters
    public int getPage() {
        return page;
    }
    
    public void setPage(int page) {
        this.page = page;
    }
    
    public int getSize() {
        return size;
    }
    
    public void setSize(int size) {
        this.size = size;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }
    
    public int getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
    
    public boolean isHasNext() {
        return hasNext;
    }
    
    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public boolean isHasPrevious() {
        return hasPrevious;
    }
    
    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
}
