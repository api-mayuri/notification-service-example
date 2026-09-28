package kz.company.notification.domain.delivery.event;

import java.time.Instant;
import java.util.UUID;

/** Провайдер принял сообщение. */
public record DeliverySent(
        UUID deliveryId,
        UUID notificationId,
        String channel, int attempt, String providerMessageId, boolean simulated,
        Instant occurredAt) implements DeliveryStatusChanged {

    @Override
    public String status() {
        return "SENT";
    }
}
