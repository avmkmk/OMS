package com.oms.notification.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NotificationSenderFactory {

    private final Map<String, NotificationSender> senderMap;

    public NotificationSenderFactory(List<NotificationSender> senders) {
        this.senderMap = senders.stream()
                .collect(Collectors.toMap(NotificationSender::getSupportedType, Function.identity()));
    }

    public NotificationSender getSender(String type) {
        return Optional.ofNullable(senderMap.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported notification type: " + type));
    }
}
