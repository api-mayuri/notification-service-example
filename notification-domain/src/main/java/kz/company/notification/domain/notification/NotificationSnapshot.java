package kz.company.notification.domain.notification;

import java.time.Instant;

/**
 * Полное состояние агрегата {@link Notification} для сохранения и восстановления.
 *
 * @param version токен оптимистической блокировки; {@code null} у ещё не сохранённого агрегата
 */
public record NotificationSnapshot(
        NotificationId id,
        NotificationRequest request,
        DeduplicationKey deduplicationKey,
        NotificationStatus status,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
