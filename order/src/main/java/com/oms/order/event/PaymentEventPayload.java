package com.oms.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventPayload {
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private String transactionId;
}
