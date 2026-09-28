package kz.company.notification.infrastructure.messaging;

import java.time.Instant;

/**
 * Сообщение для брокера в формате CloudEvents 1.0 (binary mode): атрибуты уходят в заголовки
 * {@code ce_*}, {@code payload} — JSON в значении записи.
 *
 * <p>Публикуется как событие приложения внутри транзакции: Spring Modulith сохраняет его в журнал
 * публикаций (outbox) и после коммита отправляет в Kafka в топик {@code topic} с ключом {@code key}.
 */
public record ExternalMessage(
        String topic,
        String key,
        String id,
        String source,
        String type,
        String subject,
        Instant time,
        String payload) {
}
