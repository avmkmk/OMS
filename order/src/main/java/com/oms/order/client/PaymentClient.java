package com.oms.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.oms.order.dto.PaymentRequest;
import com.oms.order.dto.PaymentResponse;
import com.oms.order.exception.ServiceUnavailableException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    /**
     * Processes payment for an order.
     *
     * @param request The payment request details.
     * @return The payment response.
     * @throws IllegalArgumentException if request is null.
     */
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Payment request cannot be null");
        }
        try {
            return restTemplate.postForObject(paymentServiceUrl + "/payments", request, PaymentResponse.class);
        } catch (HttpClientErrorException e) {
            log.error("Error processing payment: {}", e.getMessage());
            throw new RuntimeException("Failed to process payment: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling payment service", e);
            throw e;
        }
    }

    /**
     * Fallback method for payment processing when Payment service is down.
     *
     * @param request The payment request.
     * @param e       The exception that triggered the fallback.
     * @return Nothing, throws ServiceUnavailableException.
     */
    public PaymentResponse paymentFallback(PaymentRequest request, Exception e) {
        log.error("Fallback: Payment service is unavailable for order: {}. Error: {}", request.getOrderId(),
                e.getMessage());
        throw new ServiceUnavailableException("Payment service is currently busy. Please try again later.");
    }
}
