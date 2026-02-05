package com.oms.notification.event;

import com.oms.notification.model.NotificationEvent;
import com.oms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void consumeOrderEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received order event: {}", eventType);

        Map<String, Object> payload = (Map<String, Object>) event.get("payload");
        String userId = payload != null ? String.valueOf(payload.get("userId")) : "unknown";

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .payload(payload != null ? payload : Map.of())
                .build();

        notificationService.sendNotification(notificationEvent);
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void consumePaymentEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("Received payment event: {}", eventType);

        Map<String, Object> payload = (Map<String, Object>) event.get("payload");
        String orderId = payload != null ? String.valueOf(payload.get("orderId")) : "unknown";

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .eventType(eventType)
                .userId(orderId) // Using orderId as userId for simplicity
                .payload(payload != null ? payload : Map.of())
                .build();

        notificationService.sendNotification(notificationEvent);
    }
}
