package kz.company.notification.domain.delivery.event;

import java.time.Instant;
import java.util.UUID;

/** Доставка завершилась ошибкой: постоянной или после исчерпания попыток. */
public record DeliveryFailed(
        UUID deliveryId,
        UUID notificationId,
        String channel, int attempts, String reason, boolean permanent,
        Instant occurredAt) implements DeliveryStatusChanged {

    @Override
    public String status() {
        return "FAILED";
    }
}
