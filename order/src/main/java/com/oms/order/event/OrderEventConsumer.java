package com.oms.order.event;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.oms.order.service.OrderServiceImpl;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderServiceImpl orderService;

    @KafkaListener(topics = "inventory-events", groupId = "order-service-group")
    @Timed(value = "kafka.inventory.consume", description = "Process inventory events from Kafka")
    public void consumeInventoryEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Long orderId = getOrderIdFromEvent(event);

        log.info("Received inventory event: {} for order: {}", eventType, orderId);

        switch (eventType) {
            case "INVENTORY_RESERVED":
                orderService.handleInventoryReserved(orderId);
                break;
            case "INVENTORY_FAILED":
                orderService.handleInventoryFailed(orderId);
                break;
            default:
                log.warn("Unknown inventory event type: {}", eventType);
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "order-service-group")
    @Timed(value = "kafka.payment.consume", description = "Process payment events from Kafka")
    public void consumePaymentEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Long orderId = getOrderIdFromEvent(event);

        log.info("Received payment event: {} for order: {}", eventType, orderId);

        switch (eventType) {
            case "PAYMENT_SUCCESS":
                orderService.handlePaymentSuccess(orderId);
                break;
            case "PAYMENT_FAILED":
                orderService.handlePaymentFailed(orderId);
                break;
            default:
                log.warn("Unknown payment event type: {}", eventType);
        }
    }

    private Long getOrderIdFromEvent(Map<String, Object> event) {
        Object entityId = event.get("entityId");
        if (entityId instanceof Integer) {
            return ((Integer) entityId).longValue();
        }
        return (Long) entityId;
    }
}
