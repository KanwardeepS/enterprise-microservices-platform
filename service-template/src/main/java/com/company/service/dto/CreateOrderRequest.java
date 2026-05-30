package com.company.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a new order.
 */
@Schema(description = "Order creation request")
public class CreateOrderRequest {
    
    @NotBlank(message = "Customer ID is required")
    @Schema(description = "Customer identifier", example = "CUST-67890", required = true)
    private String customerId;
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    @Schema(description = "Order items", required = true)
    private List<OrderItemRequest> items;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    @Schema(description = "Special instructions or notes", example = "Please deliver before 5 PM")
    private String notes;
    
    // Constructors
    public CreateOrderRequest() {
    }
    
    public CreateOrderRequest(String customerId, List<OrderItemRequest> items, String notes) {
        this.customerId = customerId;
        this.items = items;
        this.notes = notes;
    }
    
    // Getters/Setters
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public List<OrderItemRequest> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Order item within the request.
     */
    @Schema(description = "Order item details")
    public static class OrderItemRequest {
        
        @NotBlank(message = "Product ID is required")
        @Schema(description = "Product identifier", example = "PROD-100", required = true)
        private String productId;
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        @Schema(description = "Quantity to order", example = "2", required = true)
        private Integer quantity;
        
        // Constructors
        public OrderItemRequest() {
        }
        
        public OrderItemRequest(String productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        // Getters/Setters
        public String getProductId() {
            return productId;
        }
        
        public void setProductId(String productId) {
            this.productId = productId;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
