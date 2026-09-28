package kz.company.notification.domain.delivery.event;

import java.time.Instant;
import java.util.UUID;

/** Доставка не выполнялась: отписка или нет контакта. */
public record DeliverySkipped(
        UUID deliveryId,
        UUID notificationId,
        String channel, String reason,
        Instant occurredAt) implements DeliveryStatusChanged {

    @Override
    public String status() {
        return "SKIPPED";
    }
}
