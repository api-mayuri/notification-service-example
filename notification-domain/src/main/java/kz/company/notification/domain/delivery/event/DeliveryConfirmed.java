package kz.company.notification.domain.delivery.event;

import java.time.Instant;
import java.util.UUID;

/** Провайдер подтвердил доставку получателю. */
public record DeliveryConfirmed(
        UUID deliveryId,
        UUID notificationId,
        String channel,
        Instant occurredAt) implements DeliveryStatusChanged {

    @Override
    public String status() {
        return "DELIVERED";
    }
}
