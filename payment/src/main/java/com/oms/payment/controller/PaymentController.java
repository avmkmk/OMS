package com.oms.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oms.payment.dto.PaymentRequest;
import com.oms.payment.dto.PaymentResponse;
import com.oms.payment.service.PaymentService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final MeterRegistry meterRegistry;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(paymentService.processPayment(request));
        } finally {
            sample.stop(Timer.builder("payment.process")
                    .description("Initiate payment endpoint")
                    .register(meterRegistry));
        }
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
        } finally {
            sample.stop(Timer.builder("payment.get")
                    .description("Get payment by order endpoint")
                    .register(meterRegistry));
        }
    }
}
