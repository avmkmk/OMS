package com.oms.order.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.oms.order.exception.ServiceUnavailableException;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryClientResilienceTest {

    @Autowired
    private InventoryClient inventoryClient;

    @MockBean
    private InventoryFeignClient inventoryFeignClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("inventoryService").reset();
    }

    @Test
    void testGetProductById_RetryAndFallback() {
        // Arrange
        Long productId = 1L;
        when(inventoryFeignClient.getProductById(productId))
                .thenThrow(FeignException.errorStatus("getProductById",
                        feign.Response.builder()
                                .status(500)
                                .reason("Internal Server Error")
                                .request(feign.Request.create(feign.Request.HttpMethod.GET, "/inventory/products/1",
                                        new java.util.HashMap<>(), null, java.nio.charset.StandardCharsets.UTF_8, null))
                                .build()));

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> {
            inventoryClient.getProductById(productId);
        });

        // Verify retry (max 3 attempts configured)
        verify(inventoryFeignClient, times(3)).getProductById(productId);
    }

    @Test
    void testGetProductById_CircuitBreakerOpens() {
        // Arrange
        Long productId = 1L;
        when(inventoryFeignClient.getProductById(productId))
                .thenThrow(FeignException.errorStatus("getProductById",
                        feign.Response.builder()
                                .status(500)
                                .reason("Internal Server Error")
                                .request(feign.Request.create(feign.Request.HttpMethod.GET, "/inventory/products/1",
                                        new java.util.HashMap<>(), null, java.nio.charset.StandardCharsets.UTF_8, null))
                                .build()));

        // Act: Trigger failures to reach threshold (minimumNumberOfCalls=5)
        for (int i = 0; i < 5; i++) {
            assertThrows(ServiceUnavailableException.class, () -> {
                inventoryClient.getProductById(productId);
            });
        }

        // The 6th call should be blocked by Circuit Breaker without calling FeignClient
        assertThrows(ServiceUnavailableException.class, () -> {
            inventoryClient.getProductById(productId);
        });

        // Total calls to inventoryFeignClient should be 5 (minimumNumberOfCalls threshold)
        // because Retry is the outer aspect and calls CB for each attempt.
        verify(inventoryFeignClient, times(5)).getProductById(productId);
    }
}
