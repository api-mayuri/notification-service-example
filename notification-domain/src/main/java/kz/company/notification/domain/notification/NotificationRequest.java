package kz.company.notification.domain.notification;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Данные запроса на уведомление в терминах домена. Проверяет инварианты, общие для любого
 * источника (REST, Kafka): хотя бы один канал, ограничения на метаданные.
 *
 * @param requestFingerprint отпечаток тела запроса; повтор ключа идемпотентности с другим
 *                           отпечатком — конфликт
 */
public record NotificationRequest(
        ClientId clientId,
        IdempotencyKey idempotencyKey,
        String requestFingerprint,
        Recipient recipient,
        NotificationContent content,
        Set<ChannelType> channels,
        Category category,
        Priority priority,
        Instant sendAt,
        Map<String, String> metadata) {

    public static final int MAX_CHANNELS = 16;
    public static final int MAX_METADATA_ENTRIES = 20;

    public NotificationRequest {
        Guard.notNull(clientId, "clientId");
        Guard.notNull(recipient, "recipient");
        Guard.notNull(content, "content");
        Guard.notEmpty(channels, "channels");
        if (channels.size() > MAX_CHANNELS) {
            throw DomainValidationException.invalidValue("channels", "at most " + MAX_CHANNELS + " channels allowed");
        }
        channels = Collections.unmodifiableSet(new LinkedHashSet<>(channels));
        category = Guard.notNull(category, "category");
        priority = priority == null ? Priority.NORMAL : priority;
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(metadata));
        if (metadata.size() > MAX_METADATA_ENTRIES) {
            throw DomainValidationException.invalidValue("metadata", "at most " + MAX_METADATA_ENTRIES + " entries allowed");
        }
    }

    public Optional<IdempotencyKey> optionalIdempotencyKey() {
        return Optional.ofNullable(idempotencyKey);
    }

    public Optional<Instant> optionalSendAt() {
        return Optional.ofNullable(sendAt);
    }

    public RecipientId recipientId() {
        return recipient.id();
    }
}
