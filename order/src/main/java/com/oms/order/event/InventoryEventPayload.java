package com.oms.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEventPayload {
    private Long orderId;
    private String status;
    private List<InventoryItemPayload> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItemPayload {
        private Long productId;
        private Integer quantity;
    }
}
