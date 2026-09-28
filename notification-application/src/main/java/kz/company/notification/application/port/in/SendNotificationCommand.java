package kz.company.notification.application.port.in;

import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.Category;
import kz.company.notification.domain.notification.ClientId;
import kz.company.notification.domain.notification.IdempotencyKey;
import kz.company.notification.domain.notification.NotificationContent;
import kz.company.notification.domain.notification.Priority;
import kz.company.notification.domain.notification.Recipient;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Команда «отправить уведомление». Одна и та же для всех источников (REST, Kafka):
 * входящий адаптер превращает свой формат в эту команду и вызывает {@link SendNotificationUseCase}.
 *
 * @param rawContentAllowed отправителю разрешено готовое содержимое без шаблона
 *                          (REST: scope {@code notifications:send-raw}; Kafka: доверенный источник)
 */
public record SendNotificationCommand(
        ClientId clientId,
        IdempotencyKey idempotencyKey,
        Recipient recipient,
        NotificationContent content,
        Set<ChannelType> channels,
        Category category,
        Priority priority,
        Instant sendAt,
        Map<String, String> metadata,
        boolean rawContentAllowed) {

    public SendNotificationCommand {
        Guard.notNull(clientId, "clientId");
        Guard.notNull(recipient, "recipient");
        Guard.notNull(content, "content");
        Guard.notEmpty(channels, "channels");
        Guard.notNull(category, "category");
        channels = Set.copyOf(channels);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public SendNotificationCommand withIdempotencyKey(IdempotencyKey key) {
        return new SendNotificationCommand(clientId, key, recipient, content, channels, category, priority, sendAt,
                metadata, rawContentAllowed);
    }
}
