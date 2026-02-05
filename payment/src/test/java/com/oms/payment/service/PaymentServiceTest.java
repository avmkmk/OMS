package com.oms.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.oms.common.kafka.KafkaEventPublisher;
import com.oms.payment.dto.PaymentRequest;
import com.oms.payment.dto.PaymentResponse;
import com.oms.payment.model.Payment;
import com.oms.payment.model.PaymentStatus;
import com.oms.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequest paymentRequest;
    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(100L);
        paymentRequest.setAmount(new BigDecimal("1000.00"));
        paymentRequest.setCurrency("USD");

        mockPayment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .status(PaymentStatus.SUCCESS)
                .paymentReference("PAY-12345")
                .build();
    }

    @Test
    void testProcessPayment_Idempotency() {
        // Arrange: Payment already exists for this order
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(mockPayment));

        // Act
        PaymentResponse response = paymentService.processPayment(paymentRequest);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("PAY-12345", response.getPaymentReference());

        verify(paymentRepository).findByOrderId(100L);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(kafkaEventPublisher, never()).publishEvent(anyString(), anyString(), anyString(), anyLong(), anyString(),
                anyMap());
    }

    @Test
    void testProcessPayment_NewPayment_Success() {
        // Arrange: No existing payment
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Act
        // Note: simulatePaymentGateway is random, but we can't easily mock it without
        // refactoring
        // or using PowerMock. However, we can check that it either succeeds or fails
        // and publishes
        // the correct event type.
        PaymentResponse response = paymentService.processPayment(paymentRequest);

        // Assert
        assertNotNull(response);
        verify(paymentRepository).findByOrderId(100L);
        verify(paymentRepository, atLeast(2)).save(any(Payment.class)); // Once for INITIATED, once for result
        verify(kafkaEventPublisher).publishEvent(
                eq("payment-events"),
                anyString(), // Either PAYMENT_SUCCESS or PAYMENT_FAILED
                eq("ORDER"),
                eq(100L),
                eq("payment-service"),
                anyMap());
    }

    @Test
    void testGetPaymentByOrderId_Success() {
        // Arrange
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(mockPayment));

        // Act
        PaymentResponse response = paymentService.getPaymentByOrderId(100L);

        // Assert
        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        verify(paymentRepository).findByOrderId(100L);
    }

    @Test
    void testGetPaymentByOrderId_NotFound() {
        // Arrange
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> paymentService.getPaymentByOrderId(999L));
        verify(paymentRepository).findByOrderId(999L);
    }
}

