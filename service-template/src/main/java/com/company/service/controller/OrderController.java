package com.company.service.controller;

import com.company.platform.api.ApiResponse;
import com.company.platform.api.ResourceNotFoundException;
import com.company.platform.security.RequireRoles;
import com.company.service.dto.CreateOrderRequest;
import com.company.service.dto.OrderResponse;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Order management REST API demonstrating standard conventions.
 * 
 * Features:
 * - Standard response format (ApiResponse wrapper)
 * - Comprehensive validation with Bean Validation
 * - OpenAPI/Swagger documentation
 * - Observability (logging, metrics, tracing)
 * - Role-based security
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management operations")
@RequireRoles({"USER", "ADMIN"})
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    // In-memory storage for demo purposes (replace with actual service/repository)
    private final Map<String, OrderResponse> orders = new ConcurrentHashMap<>();
    
    @Operation(
        summary = "Create a new order",
        description = "Creates a new order for the authenticated customer. Returns 201 Created with Location header."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request - validation failed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Authentication required"
        )
    })
    @PostMapping
    @Timed(value = "api.orders.create", description = "Time to create order", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Parameter(description = "Order details", required = true)
            @Valid @RequestBody CreateOrderRequest request) {
        
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        // Create order (mock implementation)
        OrderResponse order = createMockOrder(request);
        orders.put(order.getId(), order);
        
        log.info("Order created successfully: orderId={}, customerId={}, amount={}", 
            order.getId(), order.getCustomerId(), order.getTotalAmount());
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header(HttpHeaders.LOCATION, "/api/v1/orders/" + order.getId())
            .body(ApiResponse.success(order));
    }
    
    @Operation(
        summary = "Get order by ID",
        description = "Retrieves detailed information about a specific order"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Order found",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Order not found"
        )
    })
    @GetMapping("/{orderId}")
    @Timed(value = "api.orders.get", description = "Time to get order", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Order ID", example = "ORD-12345", required = true)
            @PathVariable String orderId) {
        
        log.info("Retrieving order: {}", orderId);
        
        OrderResponse order = orders.get(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        
        return ResponseEntity.ok(ApiResponse.success(order));
    }
    
    @Operation(
        summary = "List all orders",
        description = "Retrieves a paginated list of orders"
    )
    @GetMapping
    @Timed(value = "api.orders.list", description = "Time to list orders", percentiles = {0.95, 0.99})
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Listing orders: page={}, size={}", page, size);
        
        List<OrderResponse> allOrders = new ArrayList<>(orders.values());
        
        // Simple pagination for demo
        int start = Math.min(page * size, allOrders.size());
        int end = Math.min(start + size, allOrders.size());
        List<OrderResponse> pageOrders = allOrders.subList(start, end);
        
        log.info("Retrieved {} orders (page {} of {})", pageOrders.size(), page, (allOrders.size() / size) + 1);
        
        return ResponseEntity.ok(ApiResponse.success(pageOrders));
    }
    
    @Operation(
        summary = "Delete order",
        description = "Cancels and deletes an order. Only ADMIN role can delete orders."
    )
    @DeleteMapping("/{orderId}")
    @RequireRoles("ADMIN")  // More restrictive than controller-level
    @Timed(value = "api.orders.delete", description = "Time to delete order", percentiles = {0.95, 0.99})
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "Order ID", example = "ORD-12345", required = true)
            @PathVariable String orderId) {
        
        log.info("Deleting order: {}", orderId);
        
        OrderResponse order = orders.remove(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        
        log.info("Order deleted successfully: orderId={}", orderId);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Mock implementation - creates a sample order from request.
     */
    private OrderResponse createMockOrder(CreateOrderRequest request) {
        OrderResponse order = new OrderResponse();
        order.setId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerId(request.getCustomerId());
        order.setStatus("PENDING");
        order.setCurrency("USD");
        order.setNotes(request.getNotes());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        
        // Create items
        List<OrderResponse.OrderItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        
        for (int i = 0; i < request.getItems().size(); i++) {
            CreateOrderRequest.OrderItemRequest itemReq = request.getItems().get(i);
            OrderResponse.OrderItemResponse item = new OrderResponse.OrderItemResponse();
            item.setId("ITEM-" + (i + 1));
            item.setProductId(itemReq.getProductId());
            item.setProductName("Product " + itemReq.getProductId());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(new BigDecimal("49.99"));  // Mock price
            item.setLineTotal(item.getUnitPrice().multiply(new BigDecimal(itemReq.getQuantity())));
            items.add(item);
            
            total = total.add(item.getLineTotal());
        }
        
        order.setItems(items);
        order.setTotalAmount(total);
        
        return order;
    }
}
