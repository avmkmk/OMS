package com.oms.notification.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.oms.api.event.BaseEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NotificationEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void consumeOrderEvent(BaseEvent<Object> event) {
        log.info("RECEIVED EVENT from order-events: [ID: {}, Type: {}, Payload: {}]",
                event.getEntityId(), event.getEventType(), event.getPayload());

        if ("ORDER_CREATED".equals(event.getEventType())) {
            log.info(">>> Sending Notification: Order #{} created successfully!", event.getEntityId());
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "notification-service-group")
    public void consumePaymentEvent(BaseEvent<Object> event) {
        log.info("RECEIVED EVENT from payment-events: [ID: {}, Type: {}, Payload: {}]",
                event.getEntityId(), event.getEventType(), event.getPayload());

        if ("PAYMENT_SUCCESS".equals(event.getEventType())) {
            log.info(">>> Sending Notification: Payment for Order #{} received!", event.getEntityId());
        } else if ("PAYMENT_FAILED".equals(event.getEventType())) {
            log.info(">>> Sending Notification: Payment for Order #{} FAILED!", event.getEntityId());
        }
    }
}

