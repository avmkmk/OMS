package com.oms.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T> {
    private String eventId;
    private String eventType;
    private String entityType;
    private Long entityId;
    private String sourceService;
    private LocalDateTime timestamp;
    private T payload;
}
