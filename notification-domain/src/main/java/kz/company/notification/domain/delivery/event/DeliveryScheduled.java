package kz.company.notification.domain.delivery.event;

import java.time.Instant;
import java.util.UUID;

/** Доставка создана и ждёт отправки (сразу или в {@code scheduledFor}). */
public record DeliveryScheduled(
        UUID deliveryId,
        UUID notificationId,
        String recipientId, String channel, String priority, Instant scheduledFor,
        Instant occurredAt) implements DeliveryStatusChanged {

    @Override
    public String status() {
        return "PENDING";
    }
}
