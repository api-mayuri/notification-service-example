package kz.company.notification.domain.delivery;

import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.NotificationId;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.RecipientId;

import java.time.Instant;

/**
 * Полное состояние агрегата {@link Delivery} для сохранения и восстановления.
 *
 * @param address     {@code null} только у пропущенной доставки без контакта
 * @param message     {@code null} только у пропущенной доставки
 * @param dispatchedAt момент постановки в топик канала; {@code null}, если доставка ждёт планировщика
 * @param version     токен оптимистической блокировки; {@code null} у нового агрегата
 */
public record DeliverySnapshot(
        DeliveryId id,
        NotificationId notificationId,
        RecipientId recipientId,
        ChannelType channel,
        ChannelAddress address,
        RenderedMessage message,
        Category category,
        Priority priority,
        DeliveryStatus status,
        int attempts,
        Instant nextAttemptAt,
        Instant dispatchedAt,
        String lastError,
        String providerMessageId,
        boolean simulated,
        SkipReason skipReason,
        Instant sentAt,
        Instant createdAt,
        Instant updatedAt,
        Long version) {
}
