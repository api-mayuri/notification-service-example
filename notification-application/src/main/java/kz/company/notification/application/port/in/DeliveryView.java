package kz.company.notification.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Доставка для ответа API.
 *
 * @param simulated доставку выполнила заглушка провайдера: реального сообщения не было
 */
public record DeliveryView(
        UUID id,
        String channel,
        String status,
        String maskedAddress,
        int attempts,
        Instant nextAttemptAt,
        String lastError,
        String providerMessageId,
        boolean simulated,
        String skipReason,
        Integer templateVersion,
        String templateLocale,
        Instant sentAt,
        Instant updatedAt) {
}
