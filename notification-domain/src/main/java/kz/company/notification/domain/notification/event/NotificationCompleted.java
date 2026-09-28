package kz.company.notification.domain.notification.event;

import kz.company.notification.domain.common.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Все доставки уведомления завершены.
 *
 * @param status итоговый статус: COMPLETED, PARTIALLY_FAILED или FAILED
 */
public record NotificationCompleted(UUID notificationId, String status, Instant occurredAt) implements DomainEvent {
}
