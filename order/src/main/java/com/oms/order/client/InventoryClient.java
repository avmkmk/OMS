package com.oms.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.oms.order.dto.InventoryResponse;
import com.oms.order.dto.ReservationRequest;
import com.oms.order.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClient {

    private final RestTemplate restTemplate;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    /**
     * Fetches product details from the Inventory service.
     *
     * @param productId The ID of the product to fetch.
     * @return The inventory response containing product details, or null if not found.
     * @throws IllegalArgumentException if productId is null.
     */
    @CircuitBreaker(name = "inventoryService")
    @Retry(name = "inventoryService", fallbackMethod = "inventoryFallback")
    public InventoryResponse getProductById(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        try {
            return restTemplate.getForObject(inventoryServiceUrl + "/inventory/products/" + productId,
                    InventoryResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Product not found: {}", productId);
            return null;
        } catch (Exception e) {
            log.error("Error fetching product from inventory", e);
            throw e;
        }
    }

    /**
     * Reserves inventory for an order.
     *
     * @param request The reservation request details.
     * @throws IllegalArgumentException if request is null.
     */
    @CircuitBreaker(name = "inventoryService")
    @Retry(name = "inventoryService", fallbackMethod = "inventoryFallback")
    public void reserveInventory(ReservationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Reservation request cannot be null");
        }
        try {
            restTemplate.postForObject(inventoryServiceUrl + "/inventory/reserve", request, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Error reserving inventory: {}", e.getMessage());
            throw new RuntimeException("Failed to reserve inventory: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling inventory service", e);
            throw e;
        }
    }

    /**
     * Fallback method for product fetching when Inventory service is down.
     *
     * @param productId The ID of the product.
     * @param e         The exception that triggered the fallback.
     * @return Nothing, throws ServiceUnavailableException.
     */
    public InventoryResponse inventoryFallback(Long productId, Exception e) {
        log.error("Fallback: Inventory service is unavailable for product: {}. Error: {}", productId, e.getMessage());
        throw new ServiceUnavailableException("Inventory service is currently busy. Please try again later.");
    }

    /**
     * Fallback method for inventory reservation when Inventory service is down.
     *
     * @param request The reservation request.
     * @param e       The exception that triggered the fallback.
     */
    public void inventoryFallback(ReservationRequest request, Exception e) {
        log.error("Fallback: Inventory service is unavailable for order reservation: {}. Error: {}", request.getOrderId(),
                e.getMessage());
        throw new ServiceUnavailableException("Inventory service is currently busy. Please try again later.");
    }
}
