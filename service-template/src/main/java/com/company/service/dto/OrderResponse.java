package com.company.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for order details.
 */
@Schema(description = "Order details")
public class OrderResponse {
    
    @Schema(description = "Order identifier", example = "ORD-12345")
    private String id;
    
    @Schema(description = "Customer identifier", example = "CUST-67890")
    private String customerId;
    
    @Schema(description = "Order status", example = "CONFIRMED")
    private String status;
    
    @Schema(description = "Total order amount", example = "99.99")
    private BigDecimal totalAmount;
    
    @Schema(description = "Currency code", example = "USD")
    private String currency;
    
    @Schema(description = "Order items")
    private List<OrderItemResponse> items;
    
    @Schema(description = "Special instructions", example = "Please deliver before 5 PM")
    private String notes;
    
    @Schema(description = "Creation timestamp", example = "2026-04-20T10:00:00Z")
    private Instant createdAt;
    
    @Schema(description = "Last update timestamp", example = "2026-04-20T10:30:00Z")
    private Instant updatedAt;
    
    // Constructors
    public OrderResponse() {
    }
    
    // Getters/Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public List<OrderItemResponse> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Order item response.
     */
    @Schema(description = "Order item details")
    public static class OrderItemResponse {
        
        @Schema(description = "Item identifier", example = "ITEM-1")
        private String id;
        
        @Schema(description = "Product identifier", example = "PROD-100")
        private String productId;
        
        @Schema(description = "Product name", example = "Wireless Mouse")
        private String productName;
        
        @Schema(description = "Quantity ordered", example = "2")
        private Integer quantity;
        
        @Schema(description = "Unit price", example = "49.99")
        private BigDecimal unitPrice;
        
        @Schema(description = "Line total (quantity × unitPrice)", example = "99.98")
        private BigDecimal lineTotal;
        
        // Constructors
        public OrderItemResponse() {
        }
        
        // Getters/Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getProductId() {
            return productId;
        }
        
        public void setProductId(String productId) {
            this.productId = productId;
        }
        
        public String getProductName() {
            return productName;
        }
        
        public void setProductName(String productName) {
            this.productName = productName;
        }
        
        public Integer getQuantity() {
            return quantity;
        }
        
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
        
        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }
        
        public BigDecimal getLineTotal() {
            return lineTotal;
        }
        
        public void setLineTotal(BigDecimal lineTotal) {
            this.lineTotal = lineTotal;
        }
    }
}
