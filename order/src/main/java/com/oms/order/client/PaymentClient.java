package com.oms.order.client;

import com.oms.order.dto.PaymentRequest;
import com.oms.order.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final RestTemplate restTemplate;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            return restTemplate.postForObject(paymentServiceUrl + "/payments", request, PaymentResponse.class);
        } catch (HttpClientErrorException e) {
            log.error("Error processing payment: {}", e.getMessage());
            throw new RuntimeException("Failed to process payment: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling payment service", e);
            throw new RuntimeException("Error calling payment service");
        }
    }
}
