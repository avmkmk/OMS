package com.oms.notification.service;

import org.springframework.stereotype.Component;

import com.oms.notification.model.NotificationEvent;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    @Override
    public void send(NotificationEvent event) {
        String subject = "Notification from OMS";
        String body = "";

        switch (event.getEventType()) {
            case "USER_REGISTERED":
                subject = "Welcome to OMS!";
                body = "Hi " + event.getPayload().getOrDefault("name", "User") + ", thank you for registering with our system!";
                break;
            case "ORDER_COMPLETED":
                subject = "Order Successful!";
                body = "Your order #" + event.getPayload().get("orderId") + " has been processed successfully. Total: " + event.getPayload().get("totalAmount");
                break;
            default:
                body = "Received event: " + event.getEventType() + " for user: " + event.getUserId();
        }

        log.info("--- MOCK EMAIL SENT ---");
        log.info("To: {}", event.getPayload().getOrDefault("email", event.getUserId()));
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        log.info("-----------------------");
    }

    @Override
    public String getSupportedType() {
        return "EMAIL";
    }
}
