package com.oms.common.kafka;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "oms.kafka.publisher.enabled", havingValue = "true")
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

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
