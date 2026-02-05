package com.oms.order.dto;

import java.math.BigDecimal;

public record InventoryResponse(
        Long id,
        String productName,
        BigDecimal price,
        Integer quantity,
        String status) {
}
