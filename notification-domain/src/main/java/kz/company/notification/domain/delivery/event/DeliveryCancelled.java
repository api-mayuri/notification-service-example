package kz.company.notification.domain.delivery.event;

import java.time.Instant;
import java.util.UUID;

/** Доставка отменена до отправки. */
public record DeliveryCancelled(
        UUID deliveryId,
        UUID notificationId,
        String channel,
        Instant occurredAt) implements DeliveryStatusChanged {

    @Override
    public String status() {
        return "CANCELLED";
    }
}
