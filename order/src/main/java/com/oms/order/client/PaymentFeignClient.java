package com.oms.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.oms.order.dto.PaymentRequest;
import com.oms.order.dto.PaymentResponse;

@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentFeignClient {

    @PostMapping("/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}
