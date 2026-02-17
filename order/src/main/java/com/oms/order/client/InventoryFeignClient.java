package com.oms.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.oms.order.dto.InventoryResponse;
import com.oms.order.dto.ReservationRequest;

@FeignClient(name = "inventory-service", url = "${inventory.service.url}")
public interface InventoryFeignClient {

    @GetMapping("/inventory/products/{productId}")
    InventoryResponse getProductById(@PathVariable("productId") Long productId);

    @PostMapping("/inventory/reserve")
    void reserveInventory(@RequestBody ReservationRequest request);
}
