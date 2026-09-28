package kz.company.notification.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Строка результата поиска уведомлений. */
public record NotificationSummaryView(
        UUID id,
        String recipientId,
        String status,
        String category,
        String priority,
        List<String> channels,
        String templateKey,
        Instant createdAt) {
}
