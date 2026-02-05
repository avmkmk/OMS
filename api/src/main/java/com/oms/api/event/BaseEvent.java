package com.oms.api.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent<T> {
    private String eventId;
    private String eventType;
    private String entityType;
    private Long entityId;
    private String sourceService;
    private String timestamp;
    private T payload;
}
