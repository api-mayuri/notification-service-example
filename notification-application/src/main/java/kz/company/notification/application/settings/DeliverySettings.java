package kz.company.notification.application.settings;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Настройки обработки доставок.
 *
 * @param staleSendingThreshold   через сколько доставка в SENDING считается зависшей
 * @param overdueRetryGrace       насколько повтор может опоздать, прежде чем доставку вернут в очередь
 * @param batchSize               сколько доставок планировщик обрабатывает за один проход
 * @param rateLimits              лимиты частоты по каналам (нет записи — нет лимита)
 * @param idempotencyKeyRetention сколько хранить ключи идемпотентности (не меньше 24 часов)
 */
public record DeliverySettings(Duration staleSendingThreshold, Duration overdueRetryGrace, int batchSize,
                               Map<ChannelType, RateLimit> rateLimits, Duration idempotencyKeyRetention) {

    public static final Duration MIN_IDEMPOTENCY_RETENTION = Duration.ofHours(24);

    public DeliverySettings {
        Guard.notNull(staleSendingThreshold, "staleSendingThreshold");
        Guard.notNull(overdueRetryGrace, "overdueRetryGrace");
        Guard.positive(batchSize, "batchSize");
        Guard.notNull(idempotencyKeyRetention, "idempotencyKeyRetention");
        rateLimits = rateLimits == null ? Map.of() : Map.copyOf(rateLimits);
        if (idempotencyKeyRetention.compareTo(MIN_IDEMPOTENCY_RETENTION) < 0) {
            throw DomainValidationException.invalidValue("idempotencyKeyRetention", "must be at least 24 hours");
        }
    }

    public Optional<RateLimit> rateLimitFor(ChannelType channel) {
        return Optional.ofNullable(rateLimits.get(channel));
    }
}
