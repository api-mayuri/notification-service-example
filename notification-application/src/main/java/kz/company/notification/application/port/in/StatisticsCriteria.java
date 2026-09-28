package kz.company.notification.application.port.in;

import kz.company.notification.domain.common.DomainValidationException;
import kz.company.notification.domain.common.Guard;
import kz.company.notification.domain.delivery.ChannelType;
import kz.company.notification.domain.notification.ClientId;

import java.time.Duration;
import java.time.Instant;

/**
 * Параметры статистики: период, необязательный канал и шаг группировки.
 * Отправитель видит статистику только по своим уведомлениям.
 */
public record StatisticsCriteria(ClientId clientId, Instant from, Instant to, ChannelType channel,
                                 Granularity granularity) {

    public static final Duration MAX_PERIOD = Duration.ofDays(92);

    public enum Granularity {
        HOUR,
        DAY
    }

    public StatisticsCriteria {
        Guard.notNull(clientId, "clientId");
        Guard.notNull(from, "from");
        Guard.notNull(to, "to");
        granularity = granularity == null ? Granularity.DAY : granularity;
        if (!from.isBefore(to)) {
            throw new DomainValidationException("INVALID_PERIOD", "'from' must be before 'to'");
        }
        if (Duration.between(from, to).compareTo(MAX_PERIOD) > 0) {
            throw new DomainValidationException("PERIOD_TOO_LONG", "Statistics period must not exceed " + MAX_PERIOD.toDays() + " days");
        }
    }
}
