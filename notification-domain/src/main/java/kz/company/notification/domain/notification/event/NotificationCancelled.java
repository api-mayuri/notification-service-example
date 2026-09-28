package kz.company.notification.domain.notification.event;

import kz.company.notification.domain.common.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/** Уведомление отменено до отправки. */
public record NotificationCancelled(UUID notificationId, Instant occurredAt) implements DomainEvent {
}
