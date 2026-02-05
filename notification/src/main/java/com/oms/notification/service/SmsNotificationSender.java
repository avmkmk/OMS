package com.oms.notification.service;

import com.oms.notification.model.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SmsNotificationSender implements NotificationSender {

    @Override
    public void send(NotificationEvent event) {
        log.info("Sending SMS to user: {}. Payload: {}", event.getUserId(), event.getPayload());
    }

    @Override
    public String getSupportedType() {
        return "SMS";
    }
}
