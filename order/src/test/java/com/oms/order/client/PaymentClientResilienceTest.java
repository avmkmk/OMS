package com.oms.order.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.oms.order.dto.PaymentRequest;
import com.oms.order.exception.ServiceUnavailableException;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentClientResilienceTest {

    @Autowired
    private PaymentClient paymentClient;

    @MockBean
    private PaymentFeignClient paymentFeignClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("paymentService").reset();
    }

    @Test
    void testProcessPayment_RetryAndFallback() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        when(paymentFeignClient.processPayment(any(PaymentRequest.class)))
                .thenThrow(FeignException.errorStatus("processPayment",
                        feign.Response.builder()
                                .status(500)
                                .reason("Internal Server Error")
                                .request(feign.Request.create(feign.Request.HttpMethod.POST, "/payments",
                                        new java.util.HashMap<>(), null, java.nio.charset.StandardCharsets.UTF_8, null))
                                .build()));

        // Act & Assert
        assertThrows(ServiceUnavailableException.class, () -> {
            paymentClient.processPayment(request);
        });

        // Verify retry (max 3 attempts)
        verify(paymentFeignClient, times(3)).processPayment(any(PaymentRequest.class));
    }

    @Test
    void testProcessPayment_CircuitBreakerOpens() {
        // Arrange
        PaymentRequest request = new PaymentRequest();
        when(paymentFeignClient.processPayment(any(PaymentRequest.class)))
                .thenThrow(FeignException.errorStatus("processPayment",
                        feign.Response.builder()
                                .status(500)
                                .reason("Internal Server Error")
                                .request(feign.Request.create(feign.Request.HttpMethod.POST, "/payments",
                                        new java.util.HashMap<>(), null, java.nio.charset.StandardCharsets.UTF_8, null))
                                .build()));

        // Act: Trigger failures to reach threshold
        for (int i = 0; i < 2; i++) { // 2 calls * 3 retries = 6 attempts (>= 5 threshold)
            assertThrows(ServiceUnavailableException.class, () -> {
                paymentClient.processPayment(request);
            });
        }

        // The next call should be blocked
        assertThrows(ServiceUnavailableException.class, () -> {
            paymentClient.processPayment(request);
        });

        // Total calls to FeignClient should be 5
        verify(paymentFeignClient, times(5)).processPayment(any(PaymentRequest.class));
    }
}
