package kz.company.notification.domain.notification.event;

import kz.company.notification.domain.common.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Уведомление принято к отправке. */
public record NotificationAccepted(
        UUID notificationId,
        String clientId,
        String recipientId,
        String category,
        String priority,
        List<String> channels,
        Instant occurredAt) implements DomainEvent {
}
