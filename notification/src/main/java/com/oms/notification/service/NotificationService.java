package com.oms.notification.service;

import java.util.Random;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.oms.notification.model.NotificationEvent;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {

    private final NotificationSenderFactory senderFactory;
    private final Random random = new Random();

    public NotificationService(NotificationSenderFactory senderFactory) {
        this.senderFactory = senderFactory;
    }

    @Retryable(
            retryFor = { RuntimeException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    @Timed(value = "notification.send", description = "Send notification with retry")
    public void sendNotification(NotificationEvent event) {
        log.info("Attempting to send notification for event: {}", event.getEventType());
        
        // Simulate random failure (20% chance)
        if (random.nextInt(10) < 2) {
            log.error("Transient failure occurred while sending notification for user: {}", event.getUserId());
            throw new RuntimeException("Simulated transient failure");
        }

        // Determine type based on event (simulation: default to EMAIL, switch based on payload or random)
        // For v1 simulation: Let's assume the event has a favored channel or we pick one.
        // Let's use a simple heuristic: if payload contains "phone", use SMS, else EMAIL.
        String type = "EMAIL";
        if (event.getPayload() != null && event.getPayload().containsKey("phone")) {
            type = "SMS";
        }
        
        NotificationSender sender = senderFactory.getSender(type);
        sender.send(event);

        log.info("Notification sent successfully via {}! User: {}", type, event.getUserId());
    }
}
