package kz.company.notification.domain.delivery;

import java.time.Instant;
import java.util.Optional;

/**
 * Использование лимита частоты в скользящем окне.
 *
 * @param sentCount    сколько доставок отправлено в окне
 * @param oldestSentAt самая ранняя отправка в окне; {@code null}, если отправок не было
 */
public record RateWindowUsage(long sentCount, Instant oldestSentAt) {

    public static final RateWindowUsage EMPTY = new RateWindowUsage(0, null);

    public Optional<Instant> optionalOldestSentAt() {
        return Optional.ofNullable(oldestSentAt);
    }
}
