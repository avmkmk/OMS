package com.oms.notification.service;

import com.oms.notification.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    @Override
    public void send(NotificationEvent event) {
        log.info("Sending EMAIL to user: {}. Payload: {}", event.getUserId(), event.getPayload());
    }

    @Override
    public String getSupportedType() {
        return "EMAIL";
    }
}
