package com.oms.inventory.dto;

import java.math.BigDecimal;

public record InventoryDto(
        Long id,
        String productName,
        BigDecimal price,
        Integer quantity,
        String status) {
}
