package com.oms.notification.service;

import com.oms.notification.model.NotificationEvent;

public interface NotificationSender {
    /**
     * Sends the notification.
     * @param event The notification event containing details.
     */
    void send(NotificationEvent event);
    
    /**
     * Returns the supported notification type.
     * @return The supported type (e.g., EMAIL, SMS).
     */
    String getSupportedType();
}
