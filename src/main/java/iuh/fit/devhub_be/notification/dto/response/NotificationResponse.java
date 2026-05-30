package iuh.fit.devhub_be.notification.dto.response;

import iuh.fit.devhub_be.notification.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * The notification payload returned over REST and pushed over the WebSocket.
 */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        boolean read,
        UUID referenceId,
        Instant createdAt
) {}
