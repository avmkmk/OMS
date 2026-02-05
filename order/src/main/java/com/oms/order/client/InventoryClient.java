package com.oms.order.client;

import com.oms.order.dto.InventoryResponse;
import com.oms.order.dto.ReservationRequest;
import com.oms.order.exception.OrderNotFoundException; // We might need a better exception
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public InventoryResponse getProductById(Long productId) {
        try {
            return restTemplate.getForObject(inventoryServiceUrl + "/inventory/products/" + productId,
                    InventoryResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Product not found: {}", productId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching product from inventory", e);
            throw new RuntimeException("Error fetching product from inventory");
        }
    }

    public void reserveInventory(ReservationRequest request) {
        try {
            restTemplate.postForObject(inventoryServiceUrl + "/inventory/reserve", request, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Error reserving inventory: {}", e.getMessage());
            throw new RuntimeException("Failed to reserve inventory: " + e.getResponseBodyAsString());
        }
    }
}
