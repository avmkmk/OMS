package com.oms.payment.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oms.common.kafka.KafkaEventPublisher;
import com.oms.payment.dto.PaymentRequest;
import com.oms.payment.dto.PaymentResponse;
import com.oms.payment.model.Payment;
import com.oms.payment.model.PaymentStatus;
import com.oms.payment.repository.PaymentRepository;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    @Timed(value = "payment.process", description = "Process payment for an order")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());

        // Idempotency check: Does a payment already exist for this order?
        return paymentRepository.findByOrderId(request.getOrderId())
                .map(this::mapToResponse)
                .orElseGet(() -> initiateNewPayment(request));
    }

    private PaymentResponse initiateNewPayment(PaymentRequest request) {
        // 1. Create payment in INITIATED state
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.INITIATED)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment initiated with ID: {}", payment.getId());

        // 2. Mock payment processing logic
        // In a real system, this would call an external gateway (Stripe, Razorpay,
        // etc.)
        boolean isSuccess = simulatePaymentGateway();

        // 3. Update payment status based on result
        if (isSuccess) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            log.info("Payment SUCCESS for order: {}", request.getOrderId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            log.warn("Payment FAILED for order: {}", request.getOrderId());
        }

        payment = paymentRepository.save(payment);

        // Publish event
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", payment.getOrderId());
        payload.put("amount", payment.getAmount());
        payload.put("paymentStatus", payment.getStatus().name());
        payload.put("paymentReference", payment.getPaymentReference());

        String eventType = payment.getStatus() == PaymentStatus.SUCCESS ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED";
        kafkaEventPublisher.publishEvent(
                "payment-events",
                eventType,
                "ORDER",
                payment.getOrderId(),
                "payment-service",
                payload);

        return mapToResponse(payment);
    }

    private boolean simulatePaymentGateway() {
        // Simple mock: 90% success rate, or could be based on amount, etc.
        // For testing, let's keep it simple.
        return Math.random() < 0.9;
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .paymentReference(payment.getPaymentReference())
                .build();
    }
}

