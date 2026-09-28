package kz.company.notification.domain.delivery.event;

import java.time.Instant;
import java.util.UUID;

/** Временная ошибка, запланирован повтор. */
public record DeliveryRetryScheduled(
        UUID deliveryId,
        UUID notificationId,
        String channel, int attempt, Instant nextAttemptAt, String reason,
        Instant occurredAt) implements DeliveryStatusChanged {

    @Override
    public String status() {
        return "RETRY_SCHEDULED";
    }
}
