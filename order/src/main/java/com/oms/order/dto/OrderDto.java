package com.oms.order.dto;

import com.oms.order.model.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    // Request DTOs using Java Records
    public record CreateOrderRequest(
            @NotNull(message = "Items list cannot be null") @NotEmpty(message = "Items list cannot be empty") @Valid List<OrderItemRequest> items) {
    }

    public record OrderItemRequest(
            @NotNull(message = "Product ID is required") @Positive(message = "Product ID must be positive") Long productId,

            @NotBlank(message = "Product name is required") @Size(max = 255, message = "Product name must not exceed 255 characters") String productName,

            @NotNull(message = "Unit price is required") @DecimalMin(value = "0.01", message = "Unit price must be at least 0.01") BigDecimal unitPrice,

            @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
    }

    // Response DTOs using Java Records
    public record OrderResponse(
            Long id,
            Long userId,
            OrderStatus status,
            BigDecimal totalAmount,
            String currency,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemResponse> items) {
    }

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal totalPrice) {
    }

    public record OrderListResponse(
            List<OrderResponse> orders,
            int currentPage,
            int totalPages,
            long totalItems) {
    }
}
