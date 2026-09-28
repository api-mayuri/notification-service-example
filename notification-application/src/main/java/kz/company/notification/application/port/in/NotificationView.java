package kz.company.notification.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Уведомление для ответа API. Адреса получателя замаскированы. */
public record NotificationView(
        UUID id,
        String clientId,
        String recipientId,
        String status,
        String category,
        String priority,
        List<String> channels,
        String templateKey,
        Instant sendAt,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt,
        List<DeliveryView> deliveries) {
}
