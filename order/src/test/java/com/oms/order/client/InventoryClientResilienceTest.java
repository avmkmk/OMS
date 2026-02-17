package com.oms.order.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.oms.order.dto.InventoryResponse;
import com.oms.order.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

@SpringBootTest
@ActiveProfiles("test")
public class InventoryClientResilienceTest {

    @Autowired
    private InventoryClient inventoryClient;

    @MockBean
    private RestTemplate restTemplate;

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
        when(restTemplate.getForObject(any(String.class), eq(InventoryResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> {
            inventoryClient.getProductById(productId);
        });

        // Verify retry (max 3 attempts configured)
        verify(restTemplate, times(3)).getForObject(any(String.class), eq(InventoryResponse.class));
    }

    @Test
    void testGetProductById_CircuitBreakerOpens() {
        // Arrange
        Long productId = 1L;
        when(restTemplate.getForObject(any(String.class), eq(InventoryResponse.class)))
                .thenThrow(new ResourceAccessException("Service Down"));

        // Act: Trigger failures to reach threshold (minimumNumberOfCalls=5)
        for (int i = 0; i < 5; i++) {
            assertThrows(ServiceUnavailableException.class, () -> {
                inventoryClient.getProductById(productId);
            });
        }

        // The 6th call should be blocked by Circuit Breaker without calling RestTemplate
        assertThrows(ServiceUnavailableException.class, () -> {
            inventoryClient.getProductById(productId);
        });

        // Total calls to restTemplate should be 5 (minimumNumberOfCalls threshold)
        // because Retry is the outer aspect and calls CB for each attempt.
        verify(restTemplate, times(5)).getForObject(any(String.class), eq(InventoryResponse.class));
    }
}
