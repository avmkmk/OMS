package com.oms.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishEvent(String topic, String eventType, String entityType, Long entityId,
            String sourceService, Map<String, Object> payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", eventType);
        event.put("entityType", entityType);
        event.put("entityId", entityId);
        event.put("sourceService", sourceService);
        event.put("timestamp", LocalDateTime.now().toString());
        event.put("payload", payload);

        log.info("Publishing {} event to topic {} for entity: {}", eventType, topic, entityId);
        kafkaTemplate.send(topic, entityId.toString(), event);
    }
}
